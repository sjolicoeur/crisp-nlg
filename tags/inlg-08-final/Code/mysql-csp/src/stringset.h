#ifndef STRINGSET_H_
#define STRINGSET_H_

#include <set>

using namespace std;


struct ltstr
	{
	  bool operator()(const char* s1, const char* s2) const
	  {
	    return strcmp(s1, s2) < 0;
	  }
	};
	
	typedef set<const char*, ltstr> stringset;
	
#endif /*STRINGSET_H_*/
