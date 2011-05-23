#include "MysqlCSP.h"

#include <mysql++.h>



MysqlCSP::MysqlCSP() {
	conn = new mysqlpp::Connection(false);
}

MysqlCSP::~MysqlCSP() {
	delete conn;
	
	for( vector<Constraint*>::iterator it = constraints.begin(); it != constraints.end(); it++ ) {
		delete *it;
	}
}



bool MysqlCSP::connect() {
	return conn->connect("crisp", "localhost", "root");
}

// TODO gets called multiple times -- memoize results until next change
void MysqlCSP::compute_all_arguments() {
	all_arguments.clear();
	
	for( vector<Constraint*>::iterator it = constraints.begin(); it != constraints.end(); it++ ) {
		(*it)->addAllArguments(all_arguments);
	}
}
	
void MysqlCSP::addinfo(const char* cstr) {
	constraints.push_back(parseConstraint(cstr));
}

void MysqlCSP::buildQuery(stringstream& ss) {
	bool first = true;
	
	compute_all_arguments();
	
	ss << " FROM";
	for( stringset::iterator it = all_arguments.begin(); it != all_arguments.end(); it++ ) {
		if( first ) {
			first = false;
		} else {
			ss << ",";
		}
		
		ss << " object AS " << *it;
	}
	
	for( vector<Constraint*>::iterator it = constraints.begin(); it != constraints.end(); it++ ) {
		(*it)->appendFromClause(ss);
	}
	
	
	ss << "\nWHERE ";
	first = true;
	
	for( vector<Constraint*>::iterator it = constraints.begin(); it != constraints.end(); it++ ) {
		(*it)->appendWhereClause(ss, first);
		first = false;
	}
}
	
bool MysqlCSP::is_unique() {
	stringstream ss;
	
	ss << "SELECT COUNT(*)";
	buildQuery(ss);
	
	mysqlpp::Query query = conn->query();
	query << ss.str();
	mysqlpp::Result res = query.store();

	if (res) {
		int count = res.at(0).at(0);
		return (count == 1);
	} else {
		return false;
	}
	
}
	

// TODO - there is a memory leak here - whose job is it to clean up
// the memory for the duplicated strings?
void MysqlCSP::distractors(const char* arg, vector<const char*>& dlist) {
	stringstream ss;
	
	ss << "SELECT DISTINCT " << arg << ".it1";
	buildQuery(ss);
	
	// cerr << "Query: " << ss.str() << endl;
	
	dlist.clear();
	
	mysqlpp::Query query = conn->query();
	query << ss.str();
	mysqlpp::Result res = query.store();

	if (res) {
		mysqlpp::Row row;
		mysqlpp::Row::size_type i;
		for (i = 0; row = res.at(i); ++i) {
			dlist.push_back(strdup(row.at(0))); // MEMORY LEAK HERE
		}
	}
}