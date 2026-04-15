package com.njdge;

import com.njdge.obj.Fluid;
import com.njdge.obj.VehicleModel;
import org.joml.Vector2f;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SimulationAnimation extends JPanel {

    //control switches for charts
    public static final boolean ENABLE_TOTAL_FORCE_CHART = false;  // 開關：總合力長條圖
    public static final boolean ENABLE_STACKED_CHART = true;      // 開關：力量解剖堆疊圖
    public static final boolean ENABLE_AT_HISTORY_CHART = false;  // 開關：a-t 時間歷程圖

    private VehicleModel wheel;
    private final Fluid fluid;

    private final double SCALE = 100.0;
    private final double physicsDt = 0.016;
    private final int subSteps = 10;
    private final float groundY_m = 3.0f;

    private final double[] testVelocities;
    private int currentTestIndex = 0;

    private double currentMaxForce = 0.0;
    private boolean hasPrintedResults = false;

    private final DefaultCategoryDataset dataset;
    private final DefaultCategoryDataset stackedDataset;

    private final XYSeries liveSeries;
    private final Map<String, XYSeriesCollection> historyMap;
    private final JComboBox<String> historySelector;
    private double currentTime = 0.0;

    private final Color[] testColors;

    public SimulationAnimation(double[] initialVelocities_m_s,
                               DefaultCategoryDataset dataset,
                               DefaultCategoryDataset stackedDataset,
                               XYSeries liveSeries,
                               Map<String, XYSeriesCollection> historyMap,
                               JComboBox<String> historySelector) {
        this.testVelocities = initialVelocities_m_s;
        this.dataset = dataset;
        this.stackedDataset = stackedDataset;
        this.liveSeries = liveSeries;
        this.historyMap = historyMap;
        this.historySelector = historySelector;

        this.fluid = new Fluid(180,  1.9 , 4.0f, groundY_m+0.73f, 0.8);
        this.testColors = new Color[testVelocities.length];
        for (int i = 0; i < testVelocities.length; i++) {
            float hue = (float) i / testVelocities.length * 0.8f;
            testColors[i] = Color.getHSBColor(hue, 0.8f, 0.9f);
        }

        loadCurrentTest();

        setPreferredSize(new Dimension(800, 400));
        setBackground(Color.WHITE);

        Timer timer = new Timer(16, e -> {
            if(ENABLE_AT_HISTORY_CHART && liveSeries != null) liveSeries.setNotify(false);

            double dt = physicsDt / subSteps;
            for (int i = 0; i < subSteps; i++) {
                step(dt);
            }

            if(ENABLE_AT_HISTORY_CHART && liveSeries != null) liveSeries.setNotify(true);
            repaint();
        });
        timer.start();
    }

    private void loadCurrentTest() {
        double v0 = testVelocities[currentTestIndex];
        this.wheel = new VehicleModel(60, 0.7874/2);
        this.wheel.setPosition(new Vector2f(0, groundY_m - (float)wheel.getRadius()));
        this.wheel.setVelocity(new Vector2f((float) v0, 0));

        this.currentMaxForce = 0.0;
        this.currentTime = 0.0;

        if (ENABLE_AT_HISTORY_CHART && liveSeries != null) {
            liveSeries.clear();
            liveSeries.setKey(String.format("Live Data: v0 = %.0f m/s", v0));
            if (historySelector != null && historySelector.getItemCount() > 0) {
                historySelector.setSelectedIndex(0);
            }
        }
    }
    // Main simulation step
    private void step(double dt) {

        Vector2f totalForce = new Vector2f(0, (float) (wheel.getMass() * 9.81));
        double totalTorque = 0;

        Object[] fluidInteraction = fluid.getInteraction(wheel);
        Vector2f fluidForce = (Vector2f) fluidInteraction[0];

        double elasticForce = 0;
        double dampingForce = 0;
        if (fluidInteraction.length >= 4) {
            elasticForce = (Double) fluidInteraction[2];
            dampingForce = (Double) fluidInteraction[3];
        }

        totalForce.add(fluidForce);
        totalTorque += (Double) fluidInteraction[1];

        float lowestPointY = wheel.getPosition().y + (float)wheel.getRadius();
        float overlapGround = lowestPointY - groundY_m;

        if (overlapGround > 0) {
            double groundNormalForce = 50000 * overlapGround + 2000 * wheel.getVelocity().y;
            if (groundNormalForce < 0) groundNormalForce = 0;

            float contactVx = wheel.getVelocity().x - (float)(wheel.getOmega() * wheel.getRadius());
            double groundFrictionForce = -1000 * contactVx;
            double maxFriction = 0.8 * groundNormalForce;

            if (groundFrictionForce > maxFriction) groundFrictionForce = maxFriction;
            if (groundFrictionForce < -maxFriction) groundFrictionForce = -maxFriction;

            totalForce.add(new Vector2f((float)groundFrictionForce, (float)-groundNormalForce));
            totalTorque += -wheel.getRadius() * groundFrictionForce;
        }

        double currentForce = fluidForce.length();

        if (currentForce > currentMaxForce && wheel.getPosition().x < 4.8f) {
            currentMaxForce = currentForce;
            String columnKey = String.format("%.0f", testVelocities[currentTestIndex]);

            if (ENABLE_TOTAL_FORCE_CHART && dataset != null) {
                dataset.setValue(currentMaxForce, "Max Force", columnKey);
            }
            if (ENABLE_STACKED_CHART && stackedDataset != null) {
                stackedDataset.setValue(elasticForce, "Elastic Force (Shell)", columnKey);
                stackedDataset.setValue(dampingForce, "Viscous Damping (Fluid)", columnKey);
            }
        }

        wheel.update(totalForce, totalTorque, dt);

        if (ENABLE_AT_HISTORY_CHART && liveSeries != null) {
            double currentAy = -totalForce.y / wheel.getMass();
            currentTime += dt;
            liveSeries.add(currentTime, currentAy);
        }

        float epsilon = 0.05f;
        float targetY = groundY_m - (float)wheel.getRadius();
        boolean isOnGround = Math.abs(wheel.getPosition().y - targetY) < epsilon;

        if ((wheel.getPosition().x > 8.5f && isOnGround) || (wheel.getPosition().x < -0.5f && isOnGround)) {

            if (ENABLE_AT_HISTORY_CHART && liveSeries != null && historyMap != null) {
                String testKey = String.format("Test v0 = %.0f m/s", testVelocities[currentTestIndex]);
                try {
                    XYSeries clonedSeries = (XYSeries) liveSeries.clone();
                    clonedSeries.setKey(testKey);
                    XYSeriesCollection savedDataset = new XYSeriesCollection(clonedSeries);
                    historyMap.put(testKey, savedDataset);

                    boolean exists = false;
                    for (int i = 0; i < historySelector.getItemCount(); i++) {
                        if (historySelector.getItemAt(i).equals(testKey)) exists = true;
                    }
                    if (!exists) historySelector.addItem(testKey);

                } catch (CloneNotSupportedException ex) {
                    ex.printStackTrace();
                }
            }

            currentTestIndex++;

            if (currentTestIndex >= testVelocities.length) {
                if (!hasPrintedResults) {
                    printFinalSummaryTable();
                    hasPrintedResults = true;
                }
                currentTestIndex = 0;
            }
            loadCurrentTest();
        }
    }


    private void printFinalSummaryTable() {

        System.out.printf("%-15s %-15s %-20s\n", "Velocity (m/s)", "Velocity (km/h)", "Max Impact Force (N)");

        for (int i = 0; i < testVelocities.length; i++) {
            double v_ms = testVelocities[i];
            double v_kmh = v_ms * 3.6;
            String key = String.format("%.0f", v_ms);

            double forceValue = 0.0;
            if (dataset != null && ENABLE_TOTAL_FORCE_CHART) {
                Number maxForce = dataset.getValue("Max Force", key);
                forceValue = (maxForce != null) ? maxForce.doubleValue() : 0.0;
            }

            System.out.printf("%-15.1f %-15.1f %-20.1f\n", v_ms, v_kmh, forceValue);
        }
        System.out.println("==========================================================\n");
    }


    //Main rendering method
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // setting up the scene
        g2d.setColor(new Color(0, 150, 255, 150));
        int fluidRPx = (int) (fluid.getRadius() * SCALE);
        int fluidXPx = (int) (fluid.getPosition().x * SCALE);
        int fluidYPx = (int) (fluid.getPosition().y * SCALE);
        g2d.fillArc(fluidXPx - fluidRPx, fluidYPx - fluidRPx, fluidRPx * 2, fluidRPx * 2, 0, 180);

        int groundPx = (int) (groundY_m * SCALE);
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, groundPx, getWidth(), getHeight() - groundPx);

        int wheelRPx = (int) (wheel.getRadius() * SCALE);
        int wheelXPx = (int) (wheel.getPosition().x * SCALE);
        int wheelYPx = (int) (wheel.getPosition().y * SCALE);

        g2d.translate(wheelXPx, wheelYPx);
        g2d.rotate(wheel.getAngle());

        Color currentColor = testColors[currentTestIndex % testColors.length];
        g2d.setColor(currentColor);
        g2d.fillOval(-wheelRPx, -wheelRPx, wheelRPx * 2, wheelRPx * 2);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(0, 0, wheelRPx, 0);

        g2d.rotate(-wheel.getAngle());
        g2d.translate(-wheelXPx, -wheelYPx);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString(String.format("Test: %d / %d", currentTestIndex + 1, testVelocities.length), 20, 30);
        g2d.setColor(currentColor);
        g2d.drawString(String.format("Initial Velocity (v0): %.1f m/s (%.1f km/hr)", testVelocities[currentTestIndex],testVelocities[currentTestIndex]*3.6), 20, 55);
        g2d.setColor(Color.RED);
        g2d.drawString(String.format("Current Max Force: %.1f N", currentMaxForce), 20, 80);
        g2d.setColor(Color.RED);
        g2d.drawString(String.format("Current Omega: %.2f rad/s", wheel.getOmega()), 20, 105);
    }

    public static void main(String[] args) {
        ArrayList<Double> velocitiesList = new ArrayList<>();
        for (double v = 1.0; v <= 23; v += 1.0) {
            velocitiesList.add(v);
        }

        double[] testVelocities = new double[velocitiesList.size()];
        for (int i = 0; i < velocitiesList.size(); i++) {
            testVelocities[i] = velocitiesList.get(i);
        }

        DefaultCategoryDataset dataset = ENABLE_TOTAL_FORCE_CHART ? new DefaultCategoryDataset() : null;
        DefaultCategoryDataset stackedDataset = ENABLE_STACKED_CHART ? new DefaultCategoryDataset() : null;

        if (ENABLE_TOTAL_FORCE_CHART || ENABLE_STACKED_CHART) {
            for (double v : testVelocities) {
                String key = String.format("%.0f", v);
                if (dataset != null) dataset.addValue(0.0, "Max Force", key);
                if (stackedDataset != null) {
                    stackedDataset.addValue(0.0, "Elastic Force (Shell)", key);
                    stackedDataset.addValue(0.0, "Viscous Damping (Fluid)", key);
                }
            }
        }

        if (ENABLE_TOTAL_FORCE_CHART) {
            JFreeChart barChart = ChartFactory.createBarChart(
                    "Peak Total Impact Force vs Initial Velocity",
                    "Initial Velocity v0 (m/s)", "Max Force (N)",
                    dataset, PlotOrientation.VERTICAL, false, true, false
            );
            barChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
            JFrame chartFrame = new JFrame("Total Force Chart");
            chartFrame.add(new ChartPanel(barChart));
            chartFrame.pack();
            chartFrame.setLocation(100, 50);
            chartFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            chartFrame.setVisible(true);
        }

        if (ENABLE_STACKED_CHART) {
            JFreeChart stackedChart = ChartFactory.createStackedBarChart(
                    "Force Breakdown: Fluid Damping vs. Shell Elasticity",
                    "Initial Velocity v0 (m/s)", "Force Contribution (N)",
                    stackedDataset, PlotOrientation.VERTICAL, true, true, false
            );
            stackedChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
            JFrame stackedFrame = new JFrame("Force Breakdown Chart");
            stackedFrame.add(new ChartPanel(stackedChart));
            stackedFrame.pack();
            stackedFrame.setLocation(850, 50);
            stackedFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            stackedFrame.setVisible(true);
        }

        Map<String, XYSeriesCollection> historyMap = null;
        XYSeries liveSeries = null;
        JComboBox<String> historySelector = null;

        if (ENABLE_AT_HISTORY_CHART) {
            historyMap = new HashMap<>();
            liveSeries = new XYSeries("Live Data");
            XYSeriesCollection liveDataset = new XYSeriesCollection(liveSeries);

            JFreeChart atChart = ChartFactory.createXYLineChart(
                    "Vertical Acceleration Time History (a-t Graph)",
                    "Time (s)", "Acceleration a_y (m/s²)",
                    liveDataset, PlotOrientation.VERTICAL, true, true, false
            );
            atChart.getXYPlot().setBackgroundPaint(Color.WHITE);

            historySelector = new JComboBox<>();
            historySelector.addItem("--- 觀看即時模擬 (Live) ---");

            final Map<String, XYSeriesCollection> finalHistoryMap = historyMap;
            final XYSeriesCollection finalLiveDataset = liveDataset;
            final JComboBox<String> finalSelector = historySelector;

            historySelector.addActionListener(e -> {
                String selected = (String) finalSelector.getSelectedItem();
                if (selected == null || selected.startsWith("---")) {
                    atChart.getXYPlot().setDataset(finalLiveDataset);
                } else {
                    atChart.getXYPlot().setDataset(finalHistoryMap.get(selected));
                }
            });

            JFrame atFrame = new JFrame("Time History Graph");
            atFrame.setLayout(new BorderLayout());
            JPanel topPanel = new JPanel();
            topPanel.add(new JLabel("選擇要查看的試驗紀錄："));
            topPanel.add(historySelector);
            atFrame.add(topPanel, BorderLayout.NORTH);
            atFrame.add(new ChartPanel(atChart), BorderLayout.CENTER);
            atFrame.pack();
            atFrame.setLocation(850, 550);
            atFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            atFrame.setVisible(true);
        }

        JFrame animFrame = new JFrame("Real-time Simulation");
        SimulationAnimation sim = new SimulationAnimation(
                testVelocities, dataset, stackedDataset, liveSeries, historyMap, historySelector
        );
        animFrame.add(sim);
        animFrame.pack();
        animFrame.setLocation(100, 550);
        animFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        animFrame.setVisible(true);
    }
}
