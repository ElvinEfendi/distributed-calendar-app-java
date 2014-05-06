#!/usr/bin/env sh

cd jar

# check if appointments.db does not exist call database.jar to create it
file=appointments.db
if  test -s $file
then 
	echo ""
else 
	echo "Database file was not found. Creating..."
	java -jar database.jar

fi

java -jar calendar.jar $@
