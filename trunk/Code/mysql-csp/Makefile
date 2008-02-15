

# define MYSQL_LIB_DIR
# define MYSQL_INCL_DIR
# define MYSQLPP_LIB_DIR

# on my Mac:
# export MYSQL_LIB_DIR=/usr/local/mysql-5.0.45-osx10.4-i686/lib
# export MYSQLPP_LIB_DIR=/usr/local/mysql++-2.3.2
# export MYSQL_INCL_DIR=/usr/local/mysql-5.0.45-osx10.4-i686/include
# export DYLD_LIBRARY_PATH=.:$MYSQL_LIB_DIR:$MYSQLPP_LIB_DIR

# on my DICE:
# export MYSQL_LIB_DIR=/usr/lib/mysql
# export MYSQLPP_LIB_DIR=/home/v1akolle/proj/crisp/pks/mysql++-2.3.2
# export MYSQL_INCL_DIR=/usr/include/mysql
# export LD_LIBRARY_PATH=.:$MYSQL_LIB_DIR:$MYSQLPP_LIB_DIR







UNAME = $(shell uname)

ifeq ($(UNAME), Linux)
SO_EXT=so
DYNAMIC_FLAG=-shared
endif

ifeq ($(UNAME), Darwin)
SO_EXT=dylib
DYNAMIC_FLAG=-dynamiclib
endif


LIBS=-lmysqlclient -lmysqlpp
CPPFLAGS=-I$(MYSQLPP_LIB_DIR)/lib -I$(MYSQL_INCL_DIR) -fPIC



LIB_SRC=src/Constraint.cpp src/Constraint.h src/MysqlCSP.cpp src/MysqlCPP.h \
	src/stringset.h
LIB_OBJS=src/Constraint.o src/MysqlCSP.o

LIB=libMysqlCSP.$(SO_EXT)


MAIN_SRC=src/main.cpp
MAIN_OBJS=src/main.o

MAIN=mysqlcsp



$(MAIN): $(MAIN_OBJS) $(LIB)
	g++ -L$(MYSQL_LIB_DIR) -L$(MYSQLPP_LIB_DIR) -L. -o $(MAIN) $(MAIN_OBJS) -lMysqlCSP $(LIBS)




$(LIB): $(LIB_OBJS)
	g++  -L$(MYSQL_LIB_DIR) -L$(MYSQLPP_LIB_DIR) $(DYNAMIC_FLAG) -o $(LIB) $(LIB_OBJS) $(LIBS)


clean:
	rm $(LIB_OBJS) $(LIB) $(MAIN_OBJS) $(MAIN)