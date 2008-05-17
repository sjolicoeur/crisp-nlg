#ifndef MYSQLCSP_H_
#define MYSQLCSP_H_

#include <vector>
#include <sstream>

namespace mysqlpp {
	class Connection;
}  // namespace mysqlpp

#include "Constraint.h"
#include "stringset.h"


using namespace std;

class MysqlCSP {
private:
	vector<Constraint*> constraints;
	mysqlpp::Connection *conn;
	stringset all_arguments;
	
	void compute_all_arguments();
	void buildQuery(stringstream& ss);
	
public:
	MysqlCSP();
	virtual ~MysqlCSP();
	
	bool connect();
	
	void addinfo(const char* cstr);
	
	bool is_unique();
	void distractors(const char* arg, vector<const char*>& dlist);
};

#endif /*MYSQLCSP_H_*/
