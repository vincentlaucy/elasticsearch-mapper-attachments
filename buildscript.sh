mvn compiler:compile jar:jar 
#or pack current stuff with libs into a zip and run es to install it again?

ES_INSTALL='/Users/lauchunyinvincent/es/elasticsearch-0.90.3'


mv target/elasticsearch-mapper-attachments-1.8.0-SNAPSHOT.jar $ES_INSTALL/plugins/mapper-attachments/elasticsearch-mapper-attachments-1.7.0.jar

echo "moved"
ls -ltra $ES_INSTALL/plugins/mapper-attachments/

echo "restart"

$ES_INSTALL/bin/elasticsearch -f


