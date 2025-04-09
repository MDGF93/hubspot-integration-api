#!/bin/sh
# Script de entrypoint para aguardar o ngrok e iniciar a aplicação Spring Boot

# Sair imediatamente se um comando falhar
set -e

echo "SCRIPT: Starting entrypoint script..."

echo "SCRIPT: Attempting to retrieve ngrok tunnel URL..."
NGROK_URL=""
RETRY_COUNT=0
MAX_RETRIES=12 # Espera por até 60 segundos (12 tentativas * 5s de espera)

# Loop até obter a URL do ngrok ou atingir o número máximo de tentativas
while [ -z "$NGROK_URL" ] && [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  echo "SCRIPT: Polling ngrok API (Attempt $((RETRY_COUNT + 1))/$MAX_RETRIES)..."
  # Try to get the JSON, suppress errors temporarily for checking status
  TUNNELS_JSON=$(curl -s -f http://ngrok:4040/api/tunnels)
  CURL_STATUS=$?
  echo "SCRIPT: curl status: $CURL_STATUS"

  if [ $CURL_STATUS -eq 0 ]; then
    echo "SCRIPT: curl successful, parsing JSON..."
    # Try to extract the https public URL using jq
    NGROK_URL=$(echo $TUNNELS_JSON | jq -r '.tunnels[] | select(.proto=="https") | .public_url')
    echo "SCRIPT: Potential NGROK_URL from jq: [$NGROK_URL]"

    # Verifica se a URL foi extraída corretamente (não está vazia ou nula)
    if [ -z "$NGROK_URL" ] || [ "$NGROK_URL" = "null" ]; then
      echo "SCRIPT: ngrok API accessible but tunnel not established yet. Will retry."
      NGROK_URL=""
    else
      echo "SCRIPT: Found ngrok HTTPS URL: $NGROK_URL"
      # Exit loop once URL is found
      echo "SCRIPT: URL found, breaking loop."
      break
    fi
  else
    # Se o curl falhou (código de saída diferente de 0)
    echo "SCRIPT: Could not connect to ngrok API (Status: $CURL_STATUS). Will retry."
  fi

  # Check if we need to sleep and retry
  # Only increments retry count if URL is still empty
  if [ -z "$NGROK_URL" ]; then
    RETRY_COUNT=$((RETRY_COUNT + 1))
    # Only sleep if we haven't exceeded retries
    if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
        echo "SCRIPT: URL not found yet, sleeping for 5 seconds..."
        sleep 5
    fi
  fi

  # Check for max retries explicitly after incrementing
  if [ $RETRY_COUNT -ge $MAX_RETRIES ] && [ -z "$NGROK_URL" ]; then
      echo "SCRIPT: Reached max retries without finding URL."
      break # Exit loop if max retries hit
  fi
done

echo "SCRIPT: Loop finished."

# Verifica se a URL foi obtida após as tentativas
if [ -z "$NGROK_URL" ]; then
  echo "SCRIPT ERROR: Could not obtain ngrok URL after $MAX_RETRIES attempts. Exiting."
  exit 1 # Encerra o script com erro
fi

# Constrói a URL de redirecionamento completa e a exporta como variável de ambiente
# Isso a torna disponível para o processo Java que será iniciado
export HUBSPOT_REDIRECT_URI="${NGROK_URL}/oauth/callback"
echo "SCRIPT: Setting HUBSPOT_REDIRECT_URI=${HUBSPOT_REDIRECT_URI}"

# Executa o comando original para iniciar a aplicação Spring Boot
echo "SCRIPT: Executing application: java ${JAVA_OPTS} -jar app.jar $@"
exec java ${JAVA_OPTS} -jar app.jar "$@"