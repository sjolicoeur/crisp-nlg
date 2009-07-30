/*
 * PlannerOptions.java
 *
 * Created on November 30, 2006, 1:54 PM
 */

package crisp.planner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdesktop.layout.GroupLayout.ParallelGroup;
import org.jdesktop.layout.GroupLayout.SequentialGroup;


/**
 *
 * @author  alex
 */
public class PlannerOptions extends javax.swing.JFrame implements ActionListener {
	private List<JComponent> inputfields;
	private List<JLabel> inputFieldLabels;
	private Map<JComponent,String> inputFieldNames;
	private Map<JComponent,Class> inputFieldTypes;
	
	private boolean okCancel; // true = clicked OK; false = clicked Cancel
    
    /** Creates new form PlannerOptions */
    public PlannerOptions() {
        initComponents();
        
        browseDomainButton.addActionListener(this);
        browseProblemButton.addActionListener(this);
        
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        
        inputfields = new ArrayList<JComponent>();
        inputFieldLabels = new ArrayList<JLabel>();
        inputFieldNames = new HashMap<JComponent,String>();
        inputFieldTypes = new HashMap<JComponent,Class>();
    }
    
    public synchronized Map<String,Object> showDialog() {
    	setVisible(true);
    	
    	try {
			wait();
		} catch (InterruptedException e) {
			return null;
		}
		
		if( okCancel ) {
			return getOptions();
		} else {
			return null;
		}
    }
    
    public Map<String,Object> getOptions() {
    	Map<String,Object> ret = new HashMap<String,Object>();
    	
    	ret.put("domainname", new File(domainName.getText()));
    	ret.put("problemname", new File(problemName.getText()));
    	
    	for( JComponent comp : inputFieldNames.keySet() ) {
    		Object here = null;
    		
    		if( (inputFieldTypes.get(comp) == Boolean.class) || (inputFieldTypes.get(comp) == Boolean.TYPE) ) {
    			here = Boolean.valueOf(((JCheckBox) comp).isSelected());
    		} else if( (inputFieldTypes.get(comp) == Integer.class) || (inputFieldTypes.get(comp) == Integer.TYPE) ) {
    			here = Integer.valueOf(((JTextField) comp).getText());
    		}
    		
    		ret.put(inputFieldNames.get(comp), here);
    	}
    	
    	return ret;
    }
    
    public void setDomainName(String file) {
    	domainName.setText(file);
    }
    
    public void setProblemName(String file) {
    	problemName.setText(file);
    }
    
    public void addOption(String name, String label, Class type, String defaultValue) throws Exception {
    	JComponent component = null;
    	
    	//System.err.println("Add option " + name + " (label: " + label + ") of type " + type);
    	
    	if( (type == Boolean.class) || (type == Boolean.TYPE) ) {
    		JCheckBox box = new JCheckBox();
    		
    		box.setSelected(Boolean.parseBoolean(defaultValue));
    		component = box;
    	} else if( (type == Integer.class) || (type == Integer.TYPE) ) {
    		JTextField field = new JTextField();
    		
    		if( defaultValue != null ) {
    			field.setText(defaultValue);
    		}
    		
    		component = field;
    	} else {
    		throw new Exception("Unsupported option class: " + type);
    	}
    	
    	
    	inputFieldLabels.add(new JLabel(label));
    	inputfields.add(component);
    	inputFieldNames.put(component, name);
    	inputFieldTypes.put(component, type);
    }
    
    public void computeOptionsPanel() {
    	org.jdesktop.layout.GroupLayout optionsPanelLayout = new org.jdesktop.layout.GroupLayout(optionsPanel);
    	ParallelGroup horizontalGroup = optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING);
    	ParallelGroup horizontalLabelsGroup = optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING);
    	SequentialGroup verticalGroup = optionsPanelLayout.createSequentialGroup();
    	boolean first = true;
    	
    	for( int i = 0; i < inputfields.size(); i++ ) {
    		horizontalGroup.add(inputfields.get(i));
    		horizontalLabelsGroup.add(inputFieldLabels.get(i));
    		
    		if( first ) {
    			first = false;
    		} else {
    			verticalGroup.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED);
    		}
    		
    		ParallelGroup thisVerticalGroup = optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
    		.add(inputFieldLabels.get(i))
    		.add(inputfields.get(i));
    		
    		verticalGroup.add(thisVerticalGroup);
    		verticalGroup.addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
    	}
    	
    	optionsPanel.setLayout(optionsPanelLayout);
        optionsPanelLayout.setHorizontalGroup(
            optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(optionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(horizontalLabelsGroup)
                .add(33, 33, 33)
                .add(horizontalGroup)
                )
        );

        optionsPanelLayout.setVerticalGroup(
        		optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
        		.add(verticalGroup)
        		);
        
        pack();
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        problemName = new javax.swing.JTextField();
        browseProblemButton = new javax.swing.JButton();
        browseDomainButton = new javax.swing.JButton();
        domainName = new javax.swing.JTextField();
        optionsPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Planner options");
        setResizable(false);
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Planner input files"));
        jLabel1.setText("Problem");

        jLabel2.setText("Domain");

        problemName.setText("jTextField1");

        browseProblemButton.setText("Browse...");

        browseDomainButton.setText("Browse...");

        domainName.setText("jTextField2");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(domainName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 187, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(problemName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 187, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(21, 21, 21)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(browseProblemButton)
                    .add(browseDomainButton))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(problemName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1)
                    .add(browseProblemButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(domainName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(browseDomainButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        optionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Options"));
        org.jdesktop.layout.GroupLayout optionsPanelLayout = new org.jdesktop.layout.GroupLayout(optionsPanel);
        optionsPanel.setLayout(optionsPanelLayout);
        optionsPanelLayout.setHorizontalGroup(
            optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 390, Short.MAX_VALUE)
        );
        optionsPanelLayout.setVerticalGroup(
            optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 92, Short.MAX_VALUE)
        );

        okButton.setText("OK");

        cancelButton.setText("Cancel");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, optionsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(okButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(optionsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(15, 15, 15)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(okButton)
                    .add(cancelButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    /**
     * @param args the command line arguments
     * @throws Exception 
     */
    public static void main(String args[]) throws Exception {
    	final PlannerOptions test = new PlannerOptions();

    	test.addOption("plansize", "Plan size", Integer.class, "6");
    	
    	test.addOption("gist", "Use Gist", Boolean.class, "true");
    	test.addOption("mutex", "Use mutex constraints", Boolean.class, "false");

    	test.computeOptionsPanel();
    	
    	System.err.println(test.showDialog());
    	/*
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                test.setVisible(true);
            }
        });
        */
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseDomainButton;
    private javax.swing.JButton browseProblemButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField domainName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel optionsPanel;
    private javax.swing.JTextField problemName;
    // End of variables declaration//GEN-END:variables

	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == browseDomainButton ) {
			selectFilename(domainName);
		} else if( e.getSource() == browseProblemButton ) {
			selectFilename(problemName);
			
			if( ! new File(domainName.getText()).exists() ) {
				String domainFilename = problemName.getText().replaceAll("problem", "domain");
				
				if( new File(domainFilename).exists() ) {
					domainName.setText(domainFilename);
				}
			}
		} else if( (e.getSource() == okButton) || (e.getSource() == cancelButton) ) {
			synchronized(this) {
				setVisible(false);
				okCancel = (e.getSource() == okButton);
				notifyAll();
			}
		}
		
	}
	
	private void selectFilename(JTextField namefield) {
		File f = new File(namefield.getText());
		JFileChooser chooser = new JFileChooser(f.getParentFile());
		

		if( chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ) {
			namefield.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}
    
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7603756084094119293L;
	
}
