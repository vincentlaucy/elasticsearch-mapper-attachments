watch('.*[java]') { system('echo "redeploy es"; for X in `ps acx | grep -i "bin/elasticsearch" | awk {\'print $1\'}`; do
echo "kill"$X;
kill -9  $X;
done;
./buildscript.sh &  > es.log | tee es.log

#node app.js & > node.log | tee node.log
'

) }
