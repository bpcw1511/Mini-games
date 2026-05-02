import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

/**
 * 扫雷游戏 - Java 8 语法版本
 * 完全兼容 JDK 1.8，无任何 Java 9+ 特性
 */
public class Minesweeper extends JFrame {

    // 游戏配置
    private static final int ROWS = 9;
    private static final int COLS = 9;
    private static final int TOTAL_MINES = 10;

    // 核心数据
    private boolean[][] mines;              // 雷布局
    private int[][] neighborCounts;        // 邻居雷数
    private CellState[][] states;          // 格子状态
    private JButton[][] buttons;           // 按钮网格

    // 游戏状态
    private boolean gameActive;
    private int uncoveredCount;            // 已揭开非雷格子数
    private int flaggedCount;             // 当前旗子标记数

    // UI 组件
    private JPanel gridPanel;
    private JLabel mineCounterLabel;

    // 格子状态枚举
    private enum CellState {
        COVERED,    // 未揭开
        FLAGGED,    // 标记旗子
        UNCOVERED   // 已揭开
    }

    /**
     * 用于存储坐标的内部类，替代 Java 21 的 record
     */
    private static class CellPos {
        int row;
        int col;

        CellPos(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    public Minesweeper() {
        initUI();
    }

    private void initUI() {
        setTitle("扫雷");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // ---- 顶部控制栏 ----
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        // 文本块改为普通字符串（使用 HTML 实现多行效果）
        JButton resetButton = new JButton("新游戏");
        resetButton.setToolTipText("<html>开始新一局扫雷<br>左键挖开，右键标记/取消旗子</html>");
        resetButton.addActionListener(e -> resetGame());

        mineCounterLabel = new JLabel("剩余雷: " + TOTAL_MINES);
        mineCounterLabel.setFont(new Font("Dialog", Font.BOLD, 14));

        topPanel.add(resetButton);
        topPanel.add(mineCounterLabel);
        add(topPanel, BorderLayout.NORTH);

        // ---- 中央扫雷网格 ----
        gridPanel = new JPanel();
        add(gridPanel, BorderLayout.CENTER);

        // 初始化游戏
        resetGame();

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * 重置/初始化游戏
     */
    private void resetGame() {
        gameActive = true;
        uncoveredCount = 0;
        flaggedCount = 0;
        mineCounterLabel.setText("剩余雷: " + (TOTAL_MINES - flaggedCount));

        // 重新初始化数据数组
        mines = new boolean[ROWS][COLS];
        neighborCounts = new int[ROWS][COLS];
        states = new CellState[ROWS][COLS];
        buttons = new JButton[ROWS][COLS];

        // 所有格子初始为 COVERED
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                states[i][j] = CellState.COVERED;
            }
        }

        // ----- 随机布雷 (使用内部类 CellPos 替代 record) -----
        List<CellPos> allCells = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                allCells.add(new CellPos(i, j));
            }
        }
        Collections.shuffle(allCells, new Random());
        // 选取前 TOTAL_MINES 个位置布置地雷
        for (int i = 0; i < TOTAL_MINES; i++) {
            CellPos p = allCells.get(i);
            mines[p.row][p.col] = true;
        }

        // ----- 计算邻居雷数 -----
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (!mines[i][j]) {
                    int count = 0;
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            if (di == 0 && dj == 0) continue;
                            int ni = i + di, nj = j + dj;
                            if (ni >= 0 && ni < ROWS && nj >= 0 && nj < COLS && mines[ni][nj]) {
                                count++;
                            }
                        }
                    }
                    neighborCounts[i][j] = count;
                } else {
                    neighborCounts[i][j] = -1;  // 雷格标记为 -1
                }
            }
        }

        // ----- 重新构建网格面板 -----
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(ROWS, COLS, 1, 1));

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(35, 35));
                btn.setFont(new Font("Monospaced", Font.BOLD, 16));
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setFocusable(false);

                // 初始化按钮文本
                updateButtonText(i, j, btn);

                // 为使内部类使用，声明为 final 变量（Java 8 要求 effectively final）
                final int row = i;
                final int col = j;

                // 添加鼠标监听器处理左键/右键
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        e.consume();  // 防止弹出菜单等默认行为
                        if (!gameActive) return;

                        if (isLeftMouseButton(e)) {
                            handleLeftClick(row, col);
                        } else if (isRightMouseButton(e)) {
                            handleRightClick(row, col);
                        }
                    }
                });

                buttons[i][j] = btn;
                gridPanel.add(btn);
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
        pack();  // 适应可能的大小变化
    }

    /**
     * 处理左键点击：挖开格子
     */
    private void handleLeftClick(int row, int col) {
        if (!gameActive) return;
        if (states[row][col] != CellState.COVERED) return;  // 只能挖覆盖状态的格子

        if (mines[row][col]) {
            // 踩中地雷，游戏结束
            gameOver(false);
        } else {
            // 安全格子，执行揭开操作
            revealCell(row, col);
            checkVictory();
        }
    }

    /**
     * 处理右键点击：标记/取消旗子
     */
    private void handleRightClick(int row, int col) {
        if (!gameActive) return;
        CellState current = states[row][col];

        // 传统 switch 语句（Java 8 兼容）
        switch (current) {
            case COVERED:
                states[row][col] = CellState.FLAGGED;
                flaggedCount++;
                break;
            case FLAGGED:
                states[row][col] = CellState.COVERED;
                flaggedCount--;
                break;
            case UNCOVERED:
                // 已揭开格子，不做任何操作
                return;
            default:
                // 无需处理
        }

        // 更新按钮显示
        updateButtonText(row, col, buttons[row][col]);
        // 更新剩余雷数标签
        mineCounterLabel.setText("剩余雷: " + (TOTAL_MINES - flaggedCount));
    }

    /**
     * 递归揭开格子（仅用于非雷格子）
     */
    private void revealCell(int row, int col) {
        if (!gameActive) return;
        if (states[row][col] != CellState.COVERED) return;
        if (mines[row][col]) return;  // 额外安全，不应揭开雷

        // 揭开当前格子
        states[row][col] = CellState.UNCOVERED;
        uncoveredCount++;
        updateButtonText(row, col, buttons[row][col]);

        // 如果周围雷数为0，自动揭开周围8个方向的安全格子
        if (neighborCounts[row][col] == 0) {
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if (di == 0 && dj == 0) continue;
                    int ni = row + di, nj = col + dj;
                    if (ni >= 0 && ni < ROWS && nj >= 0 && nj < COLS) {
                        // 只递归揭开覆盖状态且不是雷的格子
                        if (states[ni][nj] == CellState.COVERED && !mines[ni][nj]) {
                            revealCell(ni, nj);
                        }
                    }
                }
            }
        }
    }

    /**
     * 更新单个按钮的外观（文本、颜色等）- 使用传统 if-else 替代 switch 表达式
     */
    private void updateButtonText(int row, int col, JButton btn) {
        CellState state = states[row][col];
        boolean isMine = mines[row][col];

        // 使用 if-else 决定显示文本（替代 switch 表达式）
        String text;
        if (state == CellState.COVERED) {
            text = "";
        } else if (state == CellState.FLAGGED) {
            text = "F";
        } else { // UNCOVERED
            if (isMine) {
                text = "*";      // 失败时显示雷，正常游戏中不会揭开雷
            } else {
                int count = neighborCounts[row][col];
                text = count == 0 ? "" : String.valueOf(count);
            }
        }
        btn.setText(text);

        // 简单颜色辅助识别
        if (state == CellState.FLAGGED) {
            btn.setBackground(new Color(255, 200, 200)); // 浅红
        } else if (state == CellState.UNCOVERED) {
            btn.setBackground(new Color(220, 220, 220)); // 灰色
        } else {
            btn.setBackground(null);
        }

        // 数字颜色区分（使用 if-else 链）
        if (state == CellState.UNCOVERED && !isMine && neighborCounts[row][col] > 0) {
            int count = neighborCounts[row][col];
            if (count == 1) {
                btn.setForeground(Color.BLUE);
            } else if (count == 2) {
                btn.setForeground(new Color(0, 100, 0)); // 深绿
            } else if (count == 3) {
                btn.setForeground(Color.RED);
            } else if (count == 4) {
                btn.setForeground(new Color(0, 0, 128)); // 深蓝
            } else {
                btn.setForeground(Color.BLACK);
            }
        } else {
            btn.setForeground(Color.BLACK);
        }
    }

    /**
     * 检查是否胜利（所有非雷格子均已揭开）
     */
    private void checkVictory() {
        if (uncoveredCount == ROWS * COLS - TOTAL_MINES) {
            gameOver(true);
        }
    }

    /**
     * 游戏结束处理
     *
     * @param win true: 胜利, false: 踩雷失败
     */
    private void gameOver(boolean win) {
        if (!gameActive) return;
        gameActive = false;

        if (win) {
            JOptionPane.showMessageDialog(this, "恭喜你，排雷成功！", "胜利", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // 失败时显示所有地雷位置
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    if (mines[i][j]) {
                        // 如果是地雷，强制显示为 *
                        states[i][j] = CellState.UNCOVERED;  // 临时设置为揭开状态
                        buttons[i][j].setText("*");
                        buttons[i][j].setBackground(new Color(255, 100, 100)); // 红色背景
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "踩到地雷了！游戏结束。", "失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // 确保 GUI 创建在事件调度线程 (EDT) - 静态导入 invokeLater 可用
        SwingUtilities.invokeLater(() -> {
            Minesweeper game = new Minesweeper();
            game.setVisible(true);
        });
    }
}