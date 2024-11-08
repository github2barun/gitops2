curl -u elastic:seamless -XPUT "http://localhost:9200/_template/scc_transactions?pretty" -H 'Content-Type: application/json' -d'{
  "index_patterns": [
    "scc_transactions*"
  ],
  "settings": {
    "index": {
      "codec": "best_compression",
      "refresh_interval": "30s",
      "number_of_shards": "2",
      "auto_expand_replicas": "0-1"
    }
  },
    "mappings": {
        "properties" : {
          "transactionAmount" : {
            "type" : "float"
          }
        }
    }
}'