#!/bin/bash

WIKIDUMPURL="http://dumps.wikimedia.org/frwiki/latest/frwiki-latest-pages-articles.xml.bz2"
WIKIBBZ="frwiki-latest-pages-articles.xml.bz2"
WIKILANG="fr"

LIBDIRR="`pwd`/lib/"
PYLZMA="pylzma-0.4.4"
EGGDIR="$LIBDIRR/python2.7/site-packages/pylzma-0.4.4-py2.7-linux-x86_64.egg"
#PYLZMAPATH=`pwd`/$PYLZMADIR
PYLZMAURL="https://pypi.python.org/packages/source/p/pylzma/pylzma-0.4.4.tar.gz#md5=a2be89cb2288174ebb18bec68fa559fb"
PYLZMAARCH="pylzma-0.4.4.tar.gz"

export PYTHONPATH=$PYTHONPATH:$EGGDIR/../

PYTHON="python"

if [ ! -f frwiki-latest-pages-articles.xml ]; then
    echo "download http://dumps.wikimedia.org/frwiki/latest/frwiki-latest-pages-articles.xml.bz2 first"
fi

if [ ! -d "$LIBDIRR" ]; then
    mkdir -p "$EGGDIR"
    wget "$PYLZMAURL"
    tar xzf "$PYLZMAARCH"
    cd "$PYLZMA"
    wget "http://pypi.python.org/packages/2.7/s/setuptools/setuptools-0.6c11-py2.7.egg"
    $PYTHON setup.py install --prefix="`pwd`/../"
    cd -
    rm -rf ./bin 
    echo "done"
    echo "you can : rm -rf $PYLZMA*"
fi

bzcat "$WIKIBZ" | $PYTHON WikiExctract.py -l -s -o . -L $WIKILANG -e