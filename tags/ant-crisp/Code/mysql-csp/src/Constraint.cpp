#include "Constraint.h"

#include <string.h>

#include <iostream>
using namespace std;

Constraint::Constraint(const string& pred) {
	predicate = pred;
}

Constraint::~Constraint() {
}


void Constraint::addArgument(const string& arg) {
	arguments.push_back(arg);
	all_arguments.insert(arg.c_str());
}

void Constraint::appendFromClause(stringstream& buffer) {
	buffer << ", " << predicate << " AS " << predicate;
	appendArguments(buffer);
}

void Constraint::appendArguments(stringstream& buffer) {
	for( vector<string>::iterator it = arguments.begin(); it != arguments.end(); it++ ) {
		buffer << "_" << *it;
	}
}

void Constraint::appendWhereClause(stringstream& buffer, bool first) {
	int i = 1;
	
	for( vector<string>::iterator it = arguments.begin(); it != arguments.end(); it++ ) {
		if( !first ) {
			buffer << " AND ";
		} else {
			first = false;
		}
		
		buffer << *it << ".it1=" << predicate;
		appendArguments(buffer);
		buffer << ".it" << i;
		
		i++;
	}
}

void Constraint::addAllArguments(stringset& ss) {
	ss.insert(all_arguments.begin(), all_arguments.end());
}

Constraint* parseConstraint(const char* str) {
	char* sep = " (),";
	char* copy = new char[strlen(str)];
	char* token;
	
	Constraint* ret = NULL;
	
	strcpy(copy, str);
	token  = strtok(copy, sep);

	if( token != NULL ) {
		ret = new Constraint(token);
		
		for( token = strtok(NULL, sep); token != NULL; token = strtok(NULL, sep) ) {
			ret->addArgument(token);
		}
	}

	delete[] copy;
	return ret;
}