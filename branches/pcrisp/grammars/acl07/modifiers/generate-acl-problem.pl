
$rabbits = shift;
$properties = shift;

open GRAMMAR, ">modifiers-$rabbits-$properties-grammar.xml" or die "$!";

print GRAMMAR <<'EOP';
<?xml version="1.0" encoding="UTF-8"?>

<crisp-grammar>

  <!-- GRAMMAR -->

  <tree id="i.propername">
    <node cat="NP" index="r" sem="self">
      <leaf cat="PN" type="anchor" sem="self"/>
    </node>
  </tree>

  <tree id="i.nx0Vnx1">
    <node cat="S" index="r" sem="self">
      <leaf cat="NP" type="substitution" index="0" sem="subj" />
      <node cat="VP" sem="self">
	<leaf cat="V" type="anchor" sem="self"/>
	<leaf cat="NP" type="substitution" index="0" sem="obj" />
      </node>
    </node>
  </tree>

  <tree id="i.N">
    <leaf cat="N" type="anchor" sem="self" />
  </tree>

  <tree id="a.An">
    <node cat="N" index="r" sem="self">
      <leaf cat="A" type="anchor" sem="self"/>
      <leaf cat="N" type="foot" index="f" sem="self" />
    </node>
  </tree>

  <tree id="i.nx0Vnx1Pnx2">
    <node cat="S" index="r" sem="self">
      <leaf cat="NP" type="substitution" index="0" sem="subj"/>
      <node cat="VP" sem="self">
        <leaf cat="V" type="anchor" sem="self"/>
        <leaf cat="NP" type="substitution" index="1" sem="obj"/>
        <node cat="VP" index="e" sem="self">
          <node cat="V" index="e" sem="self">
            <leaf cat="" type="terminal" index="v" sem="self"/>
          </node>
          <node cat="PP" sem="ppobj">
            <leaf cat="P" type="lex" sem="ppobj"/>
            <leaf cat="NP" type="substitution" index="2" sem="ppobj"/>
          </node>
        </node>
      </node>
    </node>
  </tree>

  <tree id="i.Dn">   <!-- non-XTAG -->
    <node cat="NP" index="r" sem="self">
      <leaf cat="D" type="anchor" sem="self" />
      <node type="substitution" cat="N" index="0" sem="self" />
    </node>
  </tree>


  

  <!-- LEXICON -->

  <entry word='Mary' pos='PN'>
    <tree refid='i.propername'>
      <semcontent>name(self,mary)</semcontent>
    </tree>
  </entry>


  <entry word='likes' pos='V'>
    <tree refid="i.nx0Vnx1">
      <semcontent>like(self,subj,obj)</semcontent>
      <semreq>animate(subj)</semreq>
    </tree>
  </entry>



  <entry word='rabbit' pos='N'>
    <tree refid="i.N">
      <semcontent>rabbit(self)</semcontent>
    </tree>
  </entry>

  <entry word='the' pos='D'>
    <tree refid="i.Dn">
    </tree>
  </entry>

  <entry word='a' pos='D'>
    <tree refid="i.Dn">
      <pragcondition>discoursenew(self)</pragcondition>
      <prageffect>uniqueref(self)</prageffect>
    </tree>
  </entry>
EOP

for( $i = 1; $i <= $properties; $i++ ) {
  print GRAMMAR <<"EOP";

  <entry word='adj$i' pos='A'>
    <tree refid='a.An'>
      <semcontent>adj${i}(self)</semcontent>
    </tree>
  </entry>
EOP
}


print GRAMMAR "</crisp-grammar>\n";
close GRAMMAR;


########################################################################

########################################################################



open PROBLEM, ">modifiers-$rabbits-$properties-problem.xml" or die "$!";
$plansize = $properties+4;

print PROBLEM <<"EOP";
<crispproblem name="modifiers-$rabbits-$properties" 
              grammar="modifiers-$rabbits-$properties-grammar.xml" 
	      cat="S" index="e" plansize="$plansize">
  <world>animate(m)</world>
  <world>name(m,mary)</world>
  <world>like(e,m,r)</world>

  <world>rabbit(r)</world>
  <world>animate(r)</world>
EOP


for( $i = 1; $i <= $properties; $i++ ) {
  print PROBLEM "  <world>adj${i}(r)</world>\n";
}


# generate distractors
for( $i = 1; $i <= $rabbits; $i++ ) {
  print PROBLEM <<"EOP";

  <world>rabbit(r${i})</world>
  <world>animate(r${i})</world>
EOP

  if( $i <= $properties ) {
    for( $j = 1; $j <= $properties; $j++ ) {
      if( $j != $i ) {
	print PROBLEM "  <world>adj${j}(r${i})</world>\n";
      }
    }
  }
}



print PROBLEM <<'EOP';

  <commgoal>like(e,m,r)</commgoal>
</crispproblem>
EOP

close PROBLEM;
