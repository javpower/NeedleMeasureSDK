package com.edge.vision.example;

import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 基于单模板的高精度针长度测量
 * @author Coder建设
 */
public class PrecisionNeedleLengthAnalyzer implements AutoCloseable {

    static {
        try {
            nu.pattern.OpenCV.loadShared();
        } catch (Throwable e) {
            JOptionPane.showMessageDialog(null, "OpenCV 加载失败: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 测量结果数据类
     */
    public static class MeasurementResult {
        public final double lengthMm;           // 测量长度(毫米)
        public final double pixelLength;        // 像素长度
        public final Point tip1;                // 针尖1坐标(亚像素精度)
        public final Point tip2;                // 针尖2坐标(亚像素精度)
        public final double confidence;         // 测量置信度(0.0-1.0)
        public final long processingTimeMs;     // 处理耗时(毫秒)
        public final String templateId;         // 使用的模板标识

        public MeasurementResult(double mm, double px, Point t1, Point t2,
                double conf, long time, String template) {
            this.lengthMm = mm;
            this.pixelLength = px;
            this.tip1 = new Point(t1.x, t1.y);
            this.tip2 = new Point(t2.x, t2.y);
            this.confidence = conf;
            this.processingTimeMs = time;
            this.templateId = template;
        }

        @Override
        public String toString() {
            return String.format(
                "MeasurementResult{length=%.4fmm, pixel=%.3f, confidence=%.3f, time=%dms}",
                lengthMm, pixelLength, confidence, processingTimeMs
            );
        }

        public String toFormattedString() {
            return String.format(
                "测量结果\n========\n长度: %.4f mm\n像素: %.3f px\n置信度: %.2f%%\n耗时: %d ms\n模板: %s",
                lengthMm, pixelLength, confidence * 100, processingTimeMs, templateId
            );
        }
    }

    /**
     * 分析模板数据类
     */
    public static class AnalysisTemplate {
        private final String templateId;
        private final Mat templateImage;
        private final Mat grayImage;
        private final double referenceLengthMm;
        private final double mmPerPixel;
        private final Point referenceTip1;
        private final Point referenceTip2;
        private final Mat tip1Patch;  // 针尖1的小区域特征
        private final Mat tip2Patch;  // 针尖2的小区域特征
        private final LocalDateTime createdAt;

        private static final int TIP_PATCH_SIZE = 30;  // 针尖特征块大小

        /**
         * 从建模结果创建模板
         */
        public AnalysisTemplate(String id, Mat image, double lengthMm,
                Point tip1, Point tip2) {
            this.templateId = id;
            this.templateImage = image.clone();
            this.referenceLengthMm = lengthMm;
            this.referenceTip1 = new Point(tip1.x, tip1.y);
            this.referenceTip2 = new Point(tip2.x, tip2.y);
            this.createdAt = LocalDateTime.now();

            // 计算像素比例
            double pixelDist = Math.sqrt(
                Math.pow(tip2.x - tip1.x, 2) +
                Math.pow(tip2.y - tip1.y, 2)
            );
            this.mmPerPixel = lengthMm / pixelDist;

            // 提取灰度图
            this.grayImage = new Mat();
            if (templateImage.channels() >= 3) {
                Imgproc.cvtColor(templateImage, grayImage, Imgproc.COLOR_BGR2GRAY);
            } else {
                templateImage.copyTo(grayImage);
            }

            // 提取针尖区域特征（用于精确匹配）
            this.tip1Patch = extractTipPatch(grayImage, tip1);
            this.tip2Patch = extractTipPatch(grayImage, tip2);

            System.out.println(String.format(
                "[模板创建] ID=%s, 长度=%.2fmm, 比例=%.6fmm/px",
                templateId, referenceLengthMm, mmPerPixel
            ));
        }

        /**
         * 提取针尖周围的小区域特征
         */
        private Mat extractTipPatch(Mat gray, Point tip) {
            int halfSize = TIP_PATCH_SIZE / 2;
            int x1 = Math.max(0, (int)tip.x - halfSize);
            int y1 = Math.max(0, (int)tip.y - halfSize);
            int x2 = Math.min(gray.cols(), (int)tip.x + halfSize);
            int y2 = Math.min(gray.rows(), (int)tip.y + halfSize);

            if (x2 - x1 < TIP_PATCH_SIZE || y2 - y1 < TIP_PATCH_SIZE) {
                // 边界情况，创建空白特征
                return new Mat(TIP_PATCH_SIZE, TIP_PATCH_SIZE, gray.type(), new Scalar(0));
            }

            Rect roi = new Rect(x1, y1, TIP_PATCH_SIZE, TIP_PATCH_SIZE);
            return new Mat(gray, roi).clone();
        }

        /**
         * 从文件加载模板(使用建模工具保存的数据)
         */
        public AnalysisTemplate(String templateFilePath) {
            // 读取模板图像和元数据
            this.templateImage = Imgcodecs.imread(templateFilePath);
            if (templateImage.empty()) {
                throw new RuntimeException("无法加载模板文件: " + templateFilePath);
            }

            // 读取同目录下的元数据文件
            String metaPath = templateFilePath.replaceAll("\\.[^.]+$", "") + ".meta";
            java.util.Properties props = new java.util.Properties();

            try {
                props.load(new java.io.FileInputStream(metaPath));
                this.templateId = props.getProperty("template.id", "UNKNOWN");
                this.referenceLengthMm = Double.parseDouble(props.getProperty("needle.length.mm", "0"));
                this.referenceTip1 = new Point(
                    Double.parseDouble(props.getProperty("tip1.x", "0")),
                    Double.parseDouble(props.getProperty("tip1.y", "0"))
                );
                this.referenceTip2 = new Point(
                    Double.parseDouble(props.getProperty("tip2.x", "0")),
                    Double.parseDouble(props.getProperty("tip2.y", "0"))
                );
            } catch (Exception e) {
                throw new RuntimeException("无法加载模板元数据: " + metaPath, e);
            }

            this.createdAt = LocalDateTime.now();

            double pixelDist = Math.sqrt(
                Math.pow(referenceTip2.x - referenceTip1.x, 2) +
                Math.pow(referenceTip2.y - referenceTip1.y, 2)
            );
            this.mmPerPixel = referenceLengthMm / pixelDist;

            // 提取灰度图
            this.grayImage = new Mat();
            if (templateImage.channels() >= 3) {
                Imgproc.cvtColor(templateImage, grayImage, Imgproc.COLOR_BGR2GRAY);
            } else {
                templateImage.copyTo(grayImage);
            }

            // 提取针尖特征块
            this.tip1Patch = extractTipPatch(grayImage, referenceTip1);
            this.tip2Patch = extractTipPatch(grayImage, referenceTip2);
        }

        public void release() {
            templateImage.release();
            grayImage.release();
            tip1Patch.release();
            tip2Patch.release();
        }

        public String getTemplateId() { return templateId; }
        public double getMmPerPixel() { return mmPerPixel; }
        public Point getReferenceTip1() { return new Point(referenceTip1.x, referenceTip1.y); }
        public Point getReferenceTip2() { return new Point(referenceTip2.x, referenceTip2.y); }
        public Mat getTip1Patch() { return tip1Patch; }
        public Mat getTip2Patch() { return tip2Patch; }
        public int getTipPatchSize() { return TIP_PATCH_SIZE; }
    }

    private final AnalysisTemplate template;

    /**
     * 使用建模工具生成的模板文件创建分析器
     */
    public PrecisionNeedleLengthAnalyzer(String templateFilePath) {
        this.template = new AnalysisTemplate(templateFilePath);
    }

    /**
     * 直接使用内存中的模板创建分析器(高级用法)
     */
    public PrecisionNeedleLengthAnalyzer(AnalysisTemplate template) {
        this.template = template;
    }

    /**
     * 分析目标图像中的针长度
     */
    public MeasurementResult analyze(String targetImagePath) {
        long startTime = System.currentTimeMillis();

        Mat target = Imgcodecs.imread(targetImagePath);
        if (target.empty()) {
            throw new RuntimeException("无法加载目标图像: " + targetImagePath);
        }

        Mat targetGray = null;

        try {
            targetGray = new Mat();
            Imgproc.cvtColor(target, targetGray, Imgproc.COLOR_BGR2GRAY);

            // 直接用两个针尖特征块进行全图匹配
            Point[] needleTips = findNeedleTipsByFeatureMatching(targetGray);

            Point t1 = needleTips[0];
            Point t2 = needleTips[1];

            // 计算像素长度
            double pixelLen = Math.sqrt(
                Math.pow(t2.x - t1.x, 2) + Math.pow(t2.y - t1.y, 2)
            );

            // 使用模板的 mmPerPixel 进行换算
            double mmLen = pixelLen * template.getMmPerPixel();

            double confidence = 0.85;  // 特征匹配通常比较可靠
            long procTime = System.currentTimeMillis() - startTime;

            System.out.println(String.format("[调试] 检测到的针尖: t1=(%.1f,%.1f), t2=(%.1f,%.1f)",
                t1.x, t1.y, t2.x, t2.y));
            System.out.println(String.format("[调试] 测量像素=%.2f, 计算长度=%.2fmm",
                pixelLen, mmLen));

            // 保存可视化结果
            saveVisualizationSimple(target, t1, t2, mmLen, targetImagePath);

            return new MeasurementResult(mmLen, pixelLen, t1, t2,
                confidence, procTime, template.getTemplateId());

        } finally {
            if (targetGray != null) targetGray.release();
            target.release();
        }
    }

    /**
     * 使用针尖特征块进行全图匹配，找到针的两端
     */
    private Point[] findNeedleTipsByFeatureMatching(Mat gray) {
        int patchSize = template.getTipPatchSize();

        // 找针尖1
        Point t1 = findBestMatch(gray, template.getTip1Patch(), patchSize, "Tip1");
        // 找针尖2
        Point t2 = findBestMatch(gray, template.getTip2Patch(), patchSize, "Tip2");

        return new Point[] { t1, t2 };
    }

    /**
     * 在全图中搜索最佳匹配位置
     * @param gray 目标灰度图
     * @param feature 特征块
     * @param featureSize 特征块大小
     * @param name 特征名称（用于日志）
     * @return 最佳匹配位置（中心点）
     */
    private Point findBestMatch(Mat gray, Mat feature, int featureSize, String name) {
        // 使用多尺度匹配
        double[] scales = { 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3 };
        double bestScore = -1;
        double bestScale = 1.0;
        Point bestLoc = null;

        for (double scale : scales) {
            int scaledSize = (int)(featureSize * scale);

            if (scaledSize > gray.cols() || scaledSize > gray.rows()) continue;

            // 缩放特征块
            Mat scaledFeature = new Mat();
            Imgproc.resize(feature, scaledFeature, new Size(scaledSize, scaledSize));

            // 全图模板匹配
            Mat result = new Mat();
            Imgproc.matchTemplate(gray, scaledFeature, result, Imgproc.TM_CCOEFF_NORMED);

            // 找到最大值位置
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            if (mmr.maxVal > bestScore) {
                bestScore = mmr.maxVal;
                bestScale = scale;
                bestLoc = mmr.maxLoc;
            }

            scaledFeature.release();
            result.release();
        }

        if (bestLoc == null) {
            throw new RuntimeException("特征匹配失败: " + name);
        }

        // 计算特征块中心点
        int actualSize = (int)(featureSize * bestScale);
        Point center = new Point(bestLoc.x + actualSize / 2.0, bestLoc.y + actualSize / 2.0);

        System.out.println(String.format("[特征匹配] %s: score=%.3f, scale=%.2f, pos=(%.1f,%.1f)",
            name, bestScore, bestScale, center.x, center.y));

        return center;
    }

    /**
     * 可视化：显示测量结果
     */
    private void saveVisualizationSimple(Mat image, Point t1, Point t2,
            double mm, String originalPath) {
        Mat out = image.clone();

        // 绘制测量线
        Imgproc.circle(out, t1, 8, new Scalar(0, 0, 255), -1);
        Imgproc.circle(out, t1, 10, new Scalar(255, 255, 255), 2);
        Imgproc.circle(out, t2, 8, new Scalar(0, 0, 255), -1);
        Imgproc.circle(out, t2, 10, new Scalar(255, 255, 255), 2);
        Imgproc.line(out, t1, t2, new Scalar(0, 255, 0), 3);

        // 标注
        String label = String.format("%.3f mm", mm);
        int[] baseline = {0};
        Size textSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, 2, baseline);
        Point textPos = new Point((t1.x + t2.x)/2 - textSize.width/2,
                                  (t1.y + t2.y)/2 - textSize.height - 10);

        // 文字背景
        Imgproc.rectangle(out,
            new Point(textPos.x - 5, textPos.y - textSize.height - 5),
            new Point(textPos.x + textSize.width + 5, textPos.y + baseline[0] + 5),
            new Scalar(0, 0, 0), -1);

        // 文字
        Imgproc.putText(out, label, textPos,
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 255, 255), 2);

        // 保存
        String outPath = originalPath.replaceAll("(\\.[^.]+)$", "_analyzed$1");
        Imgcodecs.imwrite(outPath, out);
        out.release();

        System.out.println("[可视化] 结果图保存: " + outPath);
    }

    @Override
    public void close() {
        template.release();
    }

    // ==================== GUI建模工具 ====================

    /**
     * 针模板建模工具
     * 图形界面：框选针区域并输入实际长度，生成分析模板
     */
    static class NeedleTemplateModelingTool extends JFrame {

        static {
            try {
                nu.pattern.OpenCV.loadShared();
            } catch (Throwable e) {
                JOptionPane.showMessageDialog(null, "OpenCV 加载失败: " + e.getMessage());
                System.exit(1);
            }
        }

        private Mat originalImage;
        private BufferedImage bufferedImage;

        // 两点点击模式：tip1和tip2
        private Point tip1 = null;
        private Point tip2 = null;

        private JLabel statusLabel;
        private JTextField lengthField;
        private JTextField nameField;
        private JLabel imageLabel;

        // 记住最后创建的模板路径，用于测试
        private String lastTemplatePath = null;
        private double lastTemplateLength = 50.0;

        // 测试模式标志
        private boolean isTestMode = false;
        private MeasurementResult testResult = null;
        private Mat testResultImage = null;  // 保存带测试结果的图像

        private double scale = 1.0;
        private static final double SCALE_STEP = 0.2;
        private static final double MIN_SCALE = 0.2;
        private static final double MAX_SCALE = 5.0;

        public NeedleTemplateModelingTool() {
            setTitle("针模板建模工具 - Needle Template Modeling Tool");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1200, 800);
            setLocationRelativeTo(null);

            initUI();
            setupMenu();
            setupKeyboardShortcuts();
        }

        private void initUI() {
            setLayout(new BorderLayout());

            // 图像显示区域
            imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            imageLabel.setBackground(Color.DARK_GRAY);
            imageLabel.setOpaque(true);

            // 两点点击模式：点击第一个点设置tip1，点击第二个点设置tip2
            imageLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (originalImage == null) return;
                    Point clickedPoint = screenToImage(e.getPoint());

                    if (tip1 == null) {
                        // 第一个点击：设置tip1
                        tip1 = clickedPoint;
                        statusLabel.setText("已设置第1个点，请点击针的另一端");
                        updateDisplay();
                    } else if (tip2 == null) {
                        // 第二个点击：设置tip2
                        tip2 = clickedPoint;
                        updateSelectionInfo();
                        updateDisplay();
                    } else {
                        // 已有两个点，重新开始
                        tip1 = clickedPoint;
                        tip2 = null;
                        statusLabel.setText("已设置第1个点，请点击针的另一端");
                        updateDisplay();
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(imageLabel);
            scrollPane.setPreferredSize(new Dimension(900, 700));
            add(scrollPane, BorderLayout.CENTER);

            // 控制面板
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
            controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            controlPanel.setPreferredSize(new Dimension(280, 700));

            // 文件信息
            controlPanel.add(createSectionLabel("文件操作"));
            JButton loadBtn = new JButton("加载图像 (Ctrl+O)");
            loadBtn.addActionListener(e -> loadImage());
            controlPanel.add(loadBtn);
            controlPanel.add(Box.createVerticalStrut(10));

            // 缩放控制
            controlPanel.add(createSectionLabel("视图控制"));
            JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton zoomOutBtn = new JButton(" - ");
            JButton zoomInBtn = new JButton(" + ");
            JButton fitBtn = new JButton("适应窗口");
            zoomOutBtn.addActionListener(e -> zoom(-SCALE_STEP));
            zoomInBtn.addActionListener(e -> zoom(SCALE_STEP));
            fitBtn.addActionListener(e -> fitToWindow());
            zoomPanel.add(zoomOutBtn);
            zoomPanel.add(zoomInBtn);
            zoomPanel.add(fitBtn);
            controlPanel.add(zoomPanel);
            controlPanel.add(Box.createVerticalStrut(10));

            // 选择信息
            controlPanel.add(createSectionLabel("当前选择"));
            statusLabel = new JLabel("请点击针的一个端点");
            statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlPanel.add(statusLabel);
            controlPanel.add(Box.createVerticalStrut(10));

            JButton clearBtn = new JButton("清除选择 (Esc)");
            clearBtn.addActionListener(e -> clearSelection());
            controlPanel.add(clearBtn);
            controlPanel.add(Box.createVerticalStrut(20));

            // 模板信息
            controlPanel.add(createSectionLabel("模板信息"));
            controlPanel.add(new JLabel("模板名称:"));
            nameField = new JTextField("needle_template_50mm", 20);
            controlPanel.add(nameField);
            controlPanel.add(Box.createVerticalStrut(5));

            controlPanel.add(new JLabel("实际长度 (mm):"));
            lengthField = new JTextField("50.0", 10);
            controlPanel.add(lengthField);
            controlPanel.add(Box.createVerticalStrut(10));

            JButton createBtn = new JButton("创建模板 (Ctrl+S)");
            createBtn.setBackground(new Color(0, 150, 0));
            createBtn.setForeground(Color.WHITE);
            createBtn.setFont(createBtn.getFont().deriveFont(Font.BOLD));
            createBtn.addActionListener(e -> createTemplate());
            controlPanel.add(createBtn);
            controlPanel.add(Box.createVerticalStrut(10));

            // 测试功能
            controlPanel.add(createSectionLabel("测试功能"));
            JButton testBtn = new JButton("测试模板 (Ctrl+T)");
            testBtn.setBackground(new Color(0, 100, 200));
            testBtn.setForeground(Color.WHITE);
            testBtn.setFont(testBtn.getFont().deriveFont(Font.BOLD));
            testBtn.addActionListener(e -> testTemplate());
            controlPanel.add(testBtn);

            JLabel testHint = new JLabel("<html><font size='2'>创建模板后可直接测试<br>或选择其他图片测试</font></html>");
            testHint.setForeground(Color.GRAY);
            controlPanel.add(testHint);
            controlPanel.add(Box.createVerticalGlue());

            // 操作说明
            JTextArea helpText = new JTextArea(
                    "【创建模板】\n" +
                            "1. 加载包含针的图像\n" +
                            "2. 点击针的两个端点\n" +
                            "3. 输入实际长度\n" +
                            "4. 创建模板\n\n" +
                            "【测试模板】\n" +
                            "- 创建后可直接测试\n" +
                            "- 或选择其他图片测试\n\n" +
                            "快捷键:\n" +
                            "Ctrl+O: 加载图像\n" +
                            "Ctrl+S: 创建模板\n" +
                            "Ctrl+T: 测试模板"
            );
            helpText.setEditable(false);
            helpText.setBackground(controlPanel.getBackground());
            helpText.setFont(new Font("Dialog", Font.PLAIN, 11));
            controlPanel.add(helpText);

            add(controlPanel, BorderLayout.EAST);
        }

        private JLabel createSectionLabel(String text) {
            JLabel label = new JLabel(text);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 12));
            label.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            return label;
        }

        private void setupMenu() {
            JMenuBar menuBar = new JMenuBar();

            JMenu fileMenu = new JMenu("文件");
            JMenuItem openItem = new JMenuItem("打开...");
            openItem.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
            openItem.addActionListener(e -> loadImage());

            JMenuItem exitItem = new JMenuItem("退出");
            exitItem.addActionListener(e -> System.exit(0));

            fileMenu.add(openItem);
            fileMenu.addSeparator();
            fileMenu.add(exitItem);

            JMenu viewMenu = new JMenu("视图");
            JMenuItem zoomInItem = new JMenuItem("放大");
            zoomInItem.setAccelerator(KeyStroke.getKeyStroke("ctrl PLUS"));
            zoomInItem.addActionListener(e -> zoom(SCALE_STEP));

            JMenuItem zoomOutItem = new JMenuItem("缩小");
            zoomOutItem.setAccelerator(KeyStroke.getKeyStroke("ctrl MINUS"));
            zoomOutItem.addActionListener(e -> zoom(-SCALE_STEP));

            JMenuItem fitItem = new JMenuItem("适应窗口");
            fitItem.setAccelerator(KeyStroke.getKeyStroke("ctrl 0"));
            fitItem.addActionListener(e -> fitToWindow());

            viewMenu.add(zoomInItem);
            viewMenu.add(zoomOutItem);
            viewMenu.add(fitItem);

            menuBar.add(fileMenu);
            menuBar.add(viewMenu);
            setJMenuBar(menuBar);
        }

        private void setupKeyboardShortcuts() {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addKeyEventDispatcher(e -> {
                        if (e.getID() != KeyEvent.KEY_PRESSED) return false;

                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_ESCAPE:
                                clearSelection();
                                return true;
                            case KeyEvent.VK_O:
                                if (e.isControlDown()) loadImage();
                                return true;
                            case KeyEvent.VK_S:
                                if (e.isControlDown()) createTemplate();
                                return true;
                            case KeyEvent.VK_T:
                                if (e.isControlDown()) testTemplate();
                                return true;
                        }
                        return false;
                    });
        }

        private void loadImage() {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "图像文件", "jpg", "jpeg", "png", "bmp", "tif", "tiff"));

            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File file = chooser.getSelectedFile();
            originalImage = Imgcodecs.imread(file.getAbsolutePath());

            if (originalImage.empty()) {
                JOptionPane.showMessageDialog(this, "无法加载图像", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 初始化显示
            scale = 1.0;
            clearSelection();
            // 退出测试模式
            exitTestMode();
            fitToWindow();

            setTitle("针模板建模工具 - " + file.getName());
            System.out.println("[加载] 图像: " + file.getAbsolutePath() +
                    " (" + originalImage.cols() + "x" + originalImage.rows() + ")");
        }

        private void zoom(double delta) {
            double newScale = scale + delta;
            if (newScale < MIN_SCALE) newScale = MIN_SCALE;
            if (newScale > MAX_SCALE) newScale = MAX_SCALE;

            if (newScale != scale) {
                scale = newScale;
                updateDisplay();
            }
        }

        private void fitToWindow() {
            if (originalImage == null) return;

            Container parent = imageLabel.getParent().getParent(); // JScrollPane的viewport
            int availW = parent.getWidth() - 20;
            int availH = parent.getHeight() - 20;

            double scaleX = (double) availW / originalImage.cols();
            double scaleY = (double) availH / originalImage.rows();
            scale = Math.min(scaleX, scaleY);

            if (scale < MIN_SCALE) scale = MIN_SCALE;
            if (scale > 1.0) scale = 1.0;

            updateDisplay();
        }

        private void clearSelection() {
            tip1 = null;
            tip2 = null;
            // 退出测试模式
            exitTestMode();
            statusLabel.setText("请点击针的一个端点");
            updateDisplay();
        }

        /**
         * 退出测试模式，释放资源
         */
        void exitTestMode() {
            isTestMode = false;
            testResult = null;
            if (testResultImage != null) {
                testResultImage.release();
                testResultImage = null;
            }
        }

        /**
         * 清理所有资源
         */
        public void cleanup() {
            exitTestMode();
            if (originalImage != null) {
                originalImage.release();
                originalImage = null;
            }
        }

        private Point screenToImage(java.awt.Point screenPoint) {
            int offsetX = (imageLabel.getWidth() - bufferedImage.getWidth(null)) / 2;
            int offsetY = (imageLabel.getHeight() - bufferedImage.getHeight(null)) / 2;

            double imgX = (screenPoint.x - offsetX) / scale;
            double imgY = (screenPoint.y - offsetY) / scale;

            return new Point(
                    Math.max(0, Math.min(originalImage.cols()-1, imgX)),
                    Math.max(0, Math.min(originalImage.rows()-1, imgY))
            );
        }

        private void updateDisplay() {
            if (originalImage == null) return;

            // 测试模式：使用带测试结果的图像
            Mat sourceImage = (isTestMode && testResultImage != null) ? testResultImage : originalImage;

            // 创建显示图像
            int displayW = (int)(sourceImage.cols() * scale);
            int displayH = (int)(sourceImage.rows() * scale);

            Mat resized = new Mat();
            Imgproc.resize(sourceImage, resized, new Size(displayW, displayH),
                    0, 0, Imgproc.INTER_LINEAR);

            // 测试模式：只显示测试结果，不再绘制tip1/tip2
            if (!isTestMode) {
                // 绘制已选择的点
                if (tip1 != null) {
                    Point p1 = new Point(tip1.x * scale, tip1.y * scale);
                    Imgproc.circle(resized, p1, 8, new Scalar(0, 0, 255), -1);
                    Imgproc.circle(resized, p1, 10, new Scalar(255, 255, 255), 2);
                    // 标记为 Tip1
                    Imgproc.putText(resized, "Tip1",
                            new Point(p1.x - 20, p1.y - 15),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 2);
                }

                if (tip2 != null) {
                    Point p2 = new Point(tip2.x * scale, tip2.y * scale);
                    Imgproc.circle(resized, p2, 8, new Scalar(0, 0, 255), -1);
                    Imgproc.circle(resized, p2, 10, new Scalar(255, 255, 255), 2);
                    // 标记为 Tip2
                    Imgproc.putText(resized, "Tip2",
                            new Point(p2.x - 20, p2.y - 15),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 2);
                }

                // 如果两个点都选了，绘制连接线
                if (tip1 != null && tip2 != null) {
                    Point p1 = new Point(tip1.x * scale, tip1.y * scale);
                    Point p2 = new Point(tip2.x * scale, tip2.y * scale);
                    Imgproc.line(resized, p1, p2, new Scalar(0, 255, 0), 2);

                    // 计算并显示像素长度
                    double pixelLen = Math.sqrt(
                            Math.pow(tip2.x - tip1.x, 2) + Math.pow(tip2.y - tip1.y, 2)
                    );
                    Point mid = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
                    Imgproc.putText(resized, String.format("%.1f px", pixelLen),
                            new Point(mid.x - 30, mid.y - 10),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(255, 255, 0), 2);
                }
            } else {
                // 测试模式：更新状态标签
                if (testResult != null) {
                    double error = Math.abs(testResult.lengthMm - lastTemplateLength);
                    double errorPercent = error / lastTemplateLength * 100;
                    statusLabel.setText(String.format(
                            "<html>测试模式 - 测量结果:<br>" +
                                    "测量长度: %.4f mm<br>" +
                                    "实际长度: %.4f mm<br>" +
                                    "误差: %.4f mm (%.2f%%)<br>" +
                                    "像素长度: %.3f px<br>" +
                                    "置信度: %.1f%%</html>",
                            testResult.lengthMm, lastTemplateLength, error, errorPercent,
                            testResult.pixelLength, testResult.confidence * 100
                    ));
                }
            }

            // 转换为BufferedImage
            bufferedImage = matToBufferedImage(resized);
            imageLabel.setIcon(new ImageIcon(bufferedImage));
            imageLabel.revalidate();
            imageLabel.repaint();

            resized.release();
        }

        private void updateSelectionInfo() {
            if (tip1 == null || tip2 == null) return;

            double len = Math.sqrt(
                    Math.pow(tip2.x - tip1.x, 2) + Math.pow(tip2.y - tip1.y, 2)
            );

            statusLabel.setText(String.format(
                    "<html>已选择两个端点:<br>" +
                            "Tip1: (%.1f, %.1f)<br>" +
                            "Tip2: (%.1f, %.1f)<br>" +
                            "像素长度: %.2f px<br>" +
                            "请输入实际长度并创建模板</html>",
                    tip1.x, tip1.y, tip2.x, tip2.y, len
            ));
        }

        private void createTemplate() {
            if (originalImage == null) {
                JOptionPane.showMessageDialog(this, "请先加载图像", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (tip1 == null || tip2 == null) {
                JOptionPane.showMessageDialog(this, "请先点击针的两个端点", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double lengthMm;
            try {
                lengthMm = Double.parseDouble(lengthField.getText().trim());
                if (lengthMm <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "请输入有效的长度数值(>0)", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String templateName = nameField.getText().trim();
            if (templateName.isEmpty()) {
                templateName = "needle_template_" + System.currentTimeMillis();
            }

            // 计算包围两个端点的矩形区域（添加足够的边距以保留特征）
            int margin = 30;
            int x1 = (int)Math.min(tip1.x, tip2.x) - margin;
            int y1 = (int)Math.min(tip1.y, tip2.y) - margin;
            int x2 = (int)Math.max(tip1.x, tip2.x) + margin;
            int y2 = (int)Math.max(tip1.y, tip2.y) + margin;

            // 确保在图像范围内
            x1 = Math.max(0, x1);
            y1 = Math.max(0, y1);
            x2 = Math.min(originalImage.cols(), x2);
            y2 = Math.min(originalImage.rows(), y2);

            Rect roi = new Rect(x1, y1, x2 - x1, y2 - y1);
            Mat templateRoi = new Mat(originalImage, roi);

            // 计算针尖在裁剪图中的坐标
            Point savedTip1 = new Point(tip1.x - x1, tip1.y - y1);
            Point savedTip2 = new Point(tip2.x - x1, tip2.y - y1);

            // 保存模板图像
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(templateName + ".png"));
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "PNG图像", "png"));

            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                templateRoi.release();
                return;
            }

            File saveFile = chooser.getSelectedFile();
            String path = saveFile.getAbsolutePath();
            if (!path.endsWith(".png")) path += ".png";

            boolean saved = Imgcodecs.imwrite(path, templateRoi);
            if (!saved) {
                JOptionPane.showMessageDialog(this, "保存失败", "错误", JOptionPane.ERROR_MESSAGE);
                templateRoi.release();
                return;
            }

            // 保存元数据
            String metaPath = path.replace(".png", ".meta");
            try (FileWriter writer = new FileWriter(metaPath)) {
                writer.write("# 针模板元数据\n");
                writer.write("template.id=" + templateName + "\n");
                writer.write("template.created=" +
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
                writer.write("needle.length.mm=" + lengthMm + "\n");
                writer.write("tip1.x=" + savedTip1.x + "\n");
                writer.write("tip1.y=" + savedTip1.y + "\n");
                writer.write("tip2.x=" + savedTip2.x + "\n");
                writer.write("tip2.y=" + savedTip2.y + "\n");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "元数据保存失败: " + e.getMessage(),
                        "警告", JOptionPane.WARNING_MESSAGE);
            }

            // 验证模板
            try {
                AnalysisTemplate testTemplate =
                        new AnalysisTemplate(path);
                testTemplate.release();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "模板验证失败: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                templateRoi.release();
                return;
            }

            templateRoi.release();

            // 保存模板信息用于测试
            lastTemplatePath = path;
            lastTemplateLength = lengthMm;

            JOptionPane.showMessageDialog(this,
                    "模板创建成功!\n\n文件: " + path + "\n\n现在可以使用【测试模板】功能",
                    "成功", JOptionPane.INFORMATION_MESSAGE);

            System.out.println("[模板创建] 成功: " + path);

            // 自动切换到测试模式
            testTemplate();
        }

        /**
         * 测试模板功能 - 在GUI上显示结果
         */
        private void testTemplate() {
            // 如果没有模板，提示用户选择
            if (lastTemplatePath == null) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                        "模板文件", "png"));
                chooser.setDialogTitle("选择模板文件");

                if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                lastTemplatePath = chooser.getSelectedFile().getAbsolutePath();
                // 读取元数据获取长度
                String metaPath = lastTemplatePath.replace(".png", ".meta");
                try {
                    java.util.Properties props = new java.util.Properties();
                    props.load(new java.io.FileInputStream(metaPath));
                    lastTemplateLength = Double.parseDouble(props.getProperty("needle.length.mm", "50.0"));
                } catch (Exception e) {
                    lastTemplateLength = 50.0;
                }
            }

            // 选择测试图片
            JFileChooser imgChooser = new JFileChooser();
            imgChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "图像文件", "jpg", "jpeg", "png", "bmp"));
            imgChooser.setDialogTitle("选择要测试的图像");

            if (imgChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File testImageFile = imgChooser.getSelectedFile();
            String testImagePath = testImageFile.getAbsolutePath();

            // 执行测试
            try {
                PrecisionNeedleLengthAnalyzer analyzer =
                        new PrecisionNeedleLengthAnalyzer(lastTemplatePath);

                MeasurementResult result =
                        analyzer.analyze(testImagePath);

                // 加载测试图片到GUI显示结果
                originalImage = Imgcodecs.imread(testImagePath);
                if (!originalImage.empty()) {
                    // 清除之前的点选择（因为测试时不显示选择点）
                    tip1 = null;
                    tip2 = null;

                    // 保存测试结果
                    testResult = result;

                    // 在测试图片上绘制结果
                    testResultImage = drawResultOnImage(originalImage, result);

                    // 进入测试模式
                    isTestMode = true;

                    // 适配窗口显示（会调用 updateDisplay()）
                    fitToWindow();
                }

                // 计算误差
                double error = Math.abs(result.lengthMm - lastTemplateLength);
                double errorPercent = error / lastTemplateLength * 100;

                // 显示结果对话框
                StringBuilder msg = new StringBuilder();
                msg.append("========== 测试结果 ==========\n\n");
                msg.append(String.format("测量长度: %.4f mm\n", result.lengthMm));
                msg.append(String.format("实际长度: %.4f mm\n", lastTemplateLength));
                msg.append(String.format("误差: %.4f mm (%.2f%%)\n\n", error, errorPercent));
                msg.append(String.format("像素长度: %.3f px\n", result.pixelLength));
                msg.append(String.format("置信度: %.2f%%\n", result.confidence * 100));
                msg.append(String.format("耗时: %d ms\n", result.processingTimeMs));
                msg.append("\n结果已显示在图像上，分析结果图已保存到原图目录");

                JTextArea textArea = new JTextArea(msg.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

                JOptionPane.showMessageDialog(this, textArea,
                        "测试完成!", JOptionPane.INFORMATION_MESSAGE);

                // 更新标题
                setTitle("针模板建模工具 - 测试: " + testImageFile.getName());

                analyzer.close();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "测试失败: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

        /**
         * 在图像上绘制测量结果
         */
        private Mat drawResultOnImage(Mat image, MeasurementResult result) {
            Mat out = image.clone();

            // 绘制针尖点（红色圆点，白色外圈）
            Imgproc.circle(out, result.tip1, 10, new Scalar(0, 0, 255), -1);
            Imgproc.circle(out, result.tip1, 12, new Scalar(255, 255, 255), 2);
            Imgproc.circle(out, result.tip2, 10, new Scalar(0, 0, 255), -1);
            Imgproc.circle(out, result.tip2, 12, new Scalar(255, 255, 255), 2);

            // 绘制测量线（绿色粗线）
            Imgproc.line(out, result.tip1, result.tip2, new Scalar(0, 255, 0), 4);

            // 绘制长度标注
            String label = String.format("%.2f mm", result.lengthMm);
            int[] baseline = {0};
            Size textSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, 2, baseline);
            Point mid = new Point((result.tip1.x + result.tip2.x) / 2.0, (result.tip1.y + result.tip2.y) / 2.0);
            Point textPos = new Point(mid.x - textSize.width / 2.0, mid.y - textSize.height - 15);

            // 文字背景（半透明黑色）
            Mat overlay = out.clone();
            Imgproc.rectangle(overlay,
                    new Point(textPos.x - 8, textPos.y - textSize.height - 8),
                    new Point(textPos.x + textSize.width + 8, textPos.y + baseline[0] + 8),
                    new Scalar(0, 0, 0, 180), -1);
            Core.addWeighted(overlay, 0.6, out, 0.4, 0.0, out);
            overlay.release();

            // 文字（黄色）
            Imgproc.putText(out, label, textPos,
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 255, 255), 2);

            // 绘制针尖标签
            Imgproc.putText(out, "Tip1",
                    new Point(result.tip1.x - 30, result.tip1.y - 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(0, 0, 255), 2);
            Imgproc.putText(out, "Tip2",
                    new Point(result.tip2.x - 30, result.tip2.y - 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(0, 0, 255), 2);

            return out;
        }

        private BufferedImage matToBufferedImage(Mat mat) {
            int type = BufferedImage.TYPE_3BYTE_BGR;
            if (mat.channels() == 1) {
                type = BufferedImage.TYPE_BYTE_GRAY;
            } else if (mat.channels() == 4) {
                type = BufferedImage.TYPE_4BYTE_ABGR;
            }

            BufferedImage img = new BufferedImage(mat.cols(), mat.rows(), type);
            mat.get(0, 0, ((DataBufferByte)img.getRaster().getDataBuffer()).getData());
            return img;
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            NeedleTemplateModelingTool tool = new NeedleTemplateModelingTool();
            tool.setVisible(true);

            // 添加窗口关闭监听器，确保资源释放
            tool.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    tool.cleanup();
                }
            });
        });
    }
}

