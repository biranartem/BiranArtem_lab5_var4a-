package BiranArtem_group6_lab5_var4a;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import javax.swing.*;

public class Main{
    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

class MainFrame extends JFrame {
    private static final int WIDTH = 700;
    private static final int HEIGHT = 500;

    private JFileChooser fileChooser = null;

    private JMenuItem resetGraphicsMenuItem;

    private GraphicsDisplay display = new GraphicsDisplay();

    public MainFrame() {
        super("Мышь");
        setSize(700, 500);
        Toolkit kit = Toolkit.getDefaultToolkit();
        setLocation(((kit.getScreenSize()).width - 700) / 2, ((kit.getScreenSize()).height - 500) / 2);
        setExtendedState(6);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");
        menuBar.add(fileMenu);
        Action openGraphicsAction = new AbstractAction("Открыть файл с графиком") {
            public void actionPerformed(ActionEvent event) {
                if (fileChooser == null) {
                    fileChooser = new JFileChooser();
                    fileChooser.setCurrentDirectory(new File("."));
                }
                fileChooser.showOpenDialog(MainFrame.this);
                openGraphics(fileChooser.getSelectedFile());
            }
        };
        fileMenu.add(openGraphicsAction);
        Action resetGraphicsAction = new AbstractAction("Отменить все изменения") {
            public void actionPerformed(ActionEvent event) {
                display.reset();
            }
        };
        setJMenuBar(menuBar);
        resetGraphicsMenuItem = fileMenu.add(resetGraphicsAction);
        resetGraphicsMenuItem.setEnabled(true);
        add(display, BorderLayout.CENTER);

        JCheckBoxMenuItem fill = new JCheckBoxMenuItem("Закрасить область");
        fill.addActionListener(e -> display.setFill(fill.getState()));
        fileMenu.add(fill);
    }

    protected void openGraphics(File selectedFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(selectedFile.getAbsolutePath()));
            ArrayList<Double[]> graphicsData = new ArrayList<>();
            String line;
            String[] strings = null;

            while ((line = reader.readLine()) != null) {
                strings = line.split(" ");
            }

            Double x = 0d;
            Double y = 0d;
            for (int i = 0; i < strings.length; i++) {
                if (i % 2 == 0) {
                    x = Double.parseDouble(strings[i]);
                }
                if (i % 2 != 0) {
                    y = Double.parseDouble(strings[i]);
                    graphicsData.add(new Double[]{x, y});
                }
            }

            display.displayGraphics(graphicsData);

            reader.close();
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "Указанный файл не найден",
                    "Ошибка загрузки данных", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Ошибка чтения координат точек из файла",
                    "Ошибка загрузки данных", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Ошибка чтения координат точек из файла",
                    "Ошибка загрузки данных", JOptionPane.WARNING_MESSAGE);
            return;
        }
    }
}
