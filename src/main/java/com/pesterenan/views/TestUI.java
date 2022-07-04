package com.pesterenan.views;

import java.awt.*;
import javax.swing.*;

public class TestUI extends JFrame {

    private final Dimension mainGuiDimension = new Dimension(480, 280);
    private final JPanel mainGuiJPanel = new JPanel();

    public TestUI() throws HeadlessException {
        this.initComponents();
    }

    private void initComponents() {
        setAlwaysOnTop(true);
        setTitle("MECHPESTE.get()");
        setVisible(true);
        setResizable(false);
        setLocation(100, 100);
        setSize(mainGuiDimension);
        setContentPane(mainGuiJPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainGuiJPanel.setLayout(new BorderLayout(0, 0));
//        mainGuiJPanel.add(pnlFuncoes, BorderLayout.WEST);
//        mainGuiJPanel.add(pnlParametros, BorderLayout.CENTER);
//        mainGuiJPanel.add(pnlStatus, BorderLayout.SOUTH);
    }
}
