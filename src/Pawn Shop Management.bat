%echo off%
javac -d ../bin *.java
cd ../bin
java -cp .;../mysql-connector-j-9.2.0/mysql-connector-j-9.2.0.jar Main