import tkinter as tk
from tkinter import messagebox
import random

class Minesweeper:
    def __init__(self, master):
        self.master = master
        self.master.title("扫雷游戏")
        
        # 游戏参数
        self.size = 10      # 网格尺寸
        self.mines = 10     # 地雷数量
        self.flags = 0       # 已标记的旗子数量
        
        # 初始化游戏
        self.create_widgets()
        self.start_game()

    def create_widgets(self):
        """创建游戏界面组件"""
        # 状态显示标签
        self.status_label = tk.Label(self.master, text=f"剩余地雷: {self.mines}")
        self.status_label.grid(row=0, column=0, columnspan=self.size)
        
        # 创建按钮网格
        self.buttons = []
        for i in range(self.size):
            row = []
            for j in range(self.size):
                btn = tk.Button(self.master, width=3, height=1,
                              command=lambda x=i, y=j: self.click(x, y))
                btn.grid(row=i+1, column=j)  # +1因为第一行是状态标签
                btn.bind("<Button-3>", lambda e, x=i, y=j: self.flag(x, y))
                row.append(btn)
            self.buttons.append(row)

    def start_game(self):
        """初始化游戏数据"""
        # 初始化二维数组
        self.mine_grid = [[0 for _ in range(self.size)] for _ in range(self.size)]
        self.revealed = [[False for _ in range(self.size)] for _ in range(self.size)]
        self.game_over = False
        
        # 随机放置地雷
        mine_positions = random.sample(range(self.size*self.size), self.mines)
        for pos in mine_positions:
            row = pos // self.size
            col = pos % self.size
            self.mine_grid[row][col] = -1  # -1表示地雷

        # 计算每个格子的数字
        for i in range(self.size):
            for j in range(self.size):
                if self.mine_grid[i][j] != -1:
                    self.mine_grid[i][j] = self.count_mines(i, j)

    def count_mines(self, row, col):
        """计算指定位置周围的地雷数量"""
        count = 0
        for i in range(max(0, row-1), min(self.size, row+2)):
            for j in range(max(0, col-1), min(self.size, col+2)):
                if self.mine_grid[i][j] == -1:
                    count += 1
        return count

    def click(self, row, col):
        """处理左键点击事件"""
        if self.game_over or self.revealed[row][col]:
            return

        if self.mine_grid[row][col] == -1:  # 踩到地雷
            self.game_over = True
            self.show_all_mines()
            messagebox.showinfo("游戏结束", "你踩到地雷了！")
        else:
            self.reveal(row, col)
            if self.check_win():
                messagebox.showinfo("游戏胜利", "恭喜你扫雷成功！")

    def flag(self, row, col):
        """处理右键标记事件"""
        if not self.revealed[row][col] and not self.game_over:
            btn = self.buttons[row][col]
            if btn["text"] == "🚩":
                btn.config(text="")
                self.flags -= 1
            else:
                btn.config(text="🚩")
                self.flags += 1
            self.status_label.config(text=f"剩余地雷: {self.mines - self.flags}")

    def reveal(self, row, col):
        """揭开格子并递归揭开空白区域"""
        if not (0 <= row < self.size) or not (0 <= col < self.size):
            return
        if self.revealed[row][col]:
            return

        self.revealed[row][col] = True
        value = self.mine_grid[row][col]
        btn = self.buttons[row][col]
        
        if value > 0:
            btn.config(text=str(value), relief=tk.SUNKEN)
        elif value == 0:
            btn.config(relief=tk.SUNKEN)
            # 递归揭开周围格子
            for i in range(-1, 2):
                for j in range(-1, 2):
                    self.reveal(row+i, col+j)

    def show_all_mines(self):
        """显示所有地雷"""
        for i in range(self.size):
            for j in range(self.size):
                if self.mine_grid[i][j] == -1:
                    self.buttons[i][j].config(text="💣", bg="red")

    def check_win(self):
        """检查是否获胜"""
        uncovered = 0
        for i in range(self.size):
            for j in range(self.size):
                if self.revealed[i][j] and self.mine_grid[i][j] != -1:
                    uncovered += 1
        return uncovered == (self.size*self.size - self.mines)

if __name__ == "__main__":
    root = tk.Tk()
    game = Minesweeper(root)
    root.mainloop()