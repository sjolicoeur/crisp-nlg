#ifndef CONSTRAINT_H_
#define CONSTRAINT_H_

#include <string>
#include <vector>
#include <sstream>

#include "stringset.h"

using namespace std;



class Constraint {
private:
	string predicate;
	vector<string> arguments;
	stringset all_arguments; 
	
	void appendArguments(stringstream& buffer);
	
public:
	Constraint(const string& pred);
	virtual ~Constraint();
	
	void addArgument(const string& arg);
	
	void appendFromClause(stringstream& buffer);
	void appendWhereClause(stringstream& buffer, bool first);
	
	void addAllArguments(stringset& ss);
	
};

Constraint* parseConstraint(const char* str);




#endif /*CONSTRAINT_H_*/
