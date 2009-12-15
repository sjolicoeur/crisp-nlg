package crisp.result;

/**
 * A POS-word pair that should be substituted at some appropriate
 * anchor point in a tree.
 * 
 * @author Mark Wilding
 *
 */
public class Anchor {
	private String pos;
	private String word;
	
	public Anchor(String pos, String word) {
		this.pos = pos;
		this.word = word;
	}
	
	public boolean matches(String qpos) {
		return (qpos!=null && pos!=null && word!=null && qpos.equals(pos));
	}
	
	public String toString() {
		return "("+pos+", "+word+")";
	}
	
	public String getPos() {
		return pos;
	}
	public void setPos(String pos) {
		this.pos = pos;
	}
	
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
}
