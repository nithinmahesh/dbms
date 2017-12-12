#!/bin/bash

if [ "$#" -ne 3 ]; then
  echo "Usage: runTests.sh <source folder> <output folder> <folder name containing test cases>"
  echo "Compiles java files in <source folder> and put the class files in <output folder>"
  echo "WARNING: output folder is initially deleted and recreated!!!"
  exit 1
fi

src=$1
out=$2
cases=$3

rm -rf $out
mkdir $out

echo "compiling from " $src
javac -cp lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar:./lib/sqljdbc4.jar:$out -d $out $src/*.java

# create jar file
cd $out;
jar -cvf out.jar *
cd -;

# run actual tests
java -Dfolder=$cases -cp lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar:./lib/sqljdbc4.jar:$out/out.jar \
  org.junit.runner.JUnitCore Grader
