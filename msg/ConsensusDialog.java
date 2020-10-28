package msg;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


class ConsensusDialog extends JDialog implements ActionListener
{
    private ConsensusPanel      conPan;


    ConsensusDialog(ArrayList<String> msgAlignment, Collection<String> clustalAlignment,
                    int msgScore, int clustalScore)
    {
        setModal(false);
        conPan = new ConsensusPanel(msgAlignment, clustalAlignment, msgScore, clustalScore);
        add(conPan, BorderLayout.CENTER);
        JPanel pan = new JPanel();
        JButton okBtn = new JButton("OK");
        pan.add(okBtn);
        okBtn.addActionListener(this);
        add(pan, BorderLayout.SOUTH);
        pack();
    }


    void setParams(ArrayList<String> msgAlignment, Collection<String> clustalAlignment,
                   int msgScore, int clustalScore)
    {
        conPan.setParams(msgAlignment, clustalAlignment, msgScore, clustalScore);
    }


    public void actionPerformed(ActionEvent e)
    {
        setVisible(false);
    }
}
