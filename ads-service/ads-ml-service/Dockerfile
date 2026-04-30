FROM python:3.11-slim

WORKDIR /app

# System deps for scikit-learn / numpy
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential librdkafka-dev && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ ./app/

# Model persistence directory
RUN mkdir -p /app/models

EXPOSE 8099

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8099"]
