
#include <iostream>
#include <vector>

using namespace std;

#include "MysqlCSP.h"



int main(void) {
	MysqlCSP csp;
	vector<const char*> distractors;
	
	csp.connect();
	
	csp.addinfo("rabbit(obj)");
	csp.addinfo("hat(ppobj)");
	//csp.addinfo("contained_in(obj,ppobj)");
	
	
	
	if( csp.is_unique() ) {
		cout << "CSP is unique" << endl;
	} else {
		cout << "CSP is not unique. Distractors:" << endl;
		
		csp.distractors("obj", distractors);
		for( vector<const char*>::iterator it = distractors.begin(); it != distractors.end(); it++ ) {
			cout << " -> " << *it << endl;
		}
	}
}