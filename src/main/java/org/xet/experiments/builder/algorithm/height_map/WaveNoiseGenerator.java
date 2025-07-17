package org.xet.experiments.builder.algorithm.height_map;

import java.util.*;
import java.util.stream.IntStream;

public class WaveNoiseGenerator extends GeneratorHeightMap {
    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};
    private static final double NOISE_SCALE = 0.15;
    private static final int MAX_DUPLICATES = 10;
    private static final int MAX_ITERATIONS_MULTIPLIER = 3;

    @Override
    public int[][] getHeightMap(int width, int height) {
        int[][] heightMap = new int[width][height];
        for (int[] row : heightMap) Arrays.fill(row, -1);

        Random rand = new Random();
        PerlinNoise noise = new PerlinNoise(rand.nextInt());

        int startX = width / 2;
        int startY = height / 2;
        heightMap[startX][startY] = 0;
        int totalCells = width * height;
        int filledCells = 1;

        // Очередь с приоритетом по значению шума (чем выше шум - тем раньше обрабатывается)
        PriorityQueue<Coord> queue = new PriorityQueue<>(
            (c1, c2) -> Double.compare(c2.noise, c1.noise)
        );
        queue.add(new Coord(startX, startY, 0, noise.perlin(startX * NOISE_SCALE, startY * NOISE_SCALE)));

        // Статистика для контроля количества блоков на высоте
        Map<Integer, Integer> heightStats = new HashMap<>();
        heightStats.put(0, 1);

        int maxIterations = totalCells * MAX_ITERATIONS_MULTIPLIER;
        int iterations = 0;

        while (filledCells < totalCells && iterations++ < maxIterations) {
            if (queue.isEmpty()) {
                filledCells += spawnNewWave(heightMap, queue, noise, rand, heightStats);
            }

            Coord current = queue.poll();
            if (current == null) continue;

            // Случайный порядок обработки соседей
            List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(dirs, rand);

            for (int dir : dirs) {
                int nx = current.x + DX[dir];
                int ny = current.y + DY[dir];

                if (!isValid(nx, ny, width, height) || heightMap[nx][ny] != -1) 
                    continue;

                int newHeight = current.height + 1;
                int currentCount = heightStats.getOrDefault(newHeight, 0);

                // Вероятностный контроль дубликатов
                double probability = Math.max(0.3, 1.0 - currentCount / (double) MAX_DUPLICATES);
                if (rand.nextDouble() > probability) continue;

                heightMap[nx][ny] = newHeight;
                filledCells++;
                heightStats.put(newHeight, currentCount + 1);
                double nv = noise.perlin(nx * NOISE_SCALE, ny * NOISE_SCALE);
                queue.add(new Coord(nx, ny, newHeight, nv));
            }
        }

        // Аварийное заполнение оставшихся клеток
        if (filledCells < totalCells) {
            completeUnfilled(heightMap, heightStats);
        }

        return heightMap;
    }

    private int spawnNewWave(int[][] grid, PriorityQueue<Coord> queue, 
                            PerlinNoise noise, Random rand, 
                            Map<Integer, Integer> stats) {
        List<Coord> candidates = new ArrayList<>();
        int width = grid.length;
        int height = grid[0].length;
        int added = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] == -1) continue;
                
                for (int d = 0; d < 4; d++) {
                    int nx = x + DX[d];
                    int ny = y + DY[d];
                    
                    if (isValid(nx, ny, width, height) && grid[nx][ny] == -1) {
                        double nv = noise.perlin(nx * NOISE_SCALE, ny * NOISE_SCALE);
                        candidates.add(new Coord(nx, ny, grid[x][y] + 1, nv));
                    }
                }
            }
        }

        if (candidates.isEmpty()) return 0;
        
        // Выбираем самые "интересные" точки по шуму
        candidates.sort(Comparator.comparingDouble(c -> -c.noise));
        int toAdd = Math.min(10, Math.max(1, candidates.size() / 5));
        
        for (int i = 0; i < toAdd; i++) {
            Coord c = candidates.get(i);
            if (grid[c.x][c.y] == -1) {
                int heightValue = c.height;
                int count = stats.getOrDefault(heightValue, 0);
                
                if (count < MAX_DUPLICATES * 2) {
                    grid[c.x][c.y] = heightValue;
                    stats.put(heightValue, count + 1);
                    queue.add(c);
                    added++;
                }
            }
        }
        
        return added;
    }

    private void completeUnfilled(int[][] grid, Map<Integer, Integer> stats) {
        int width = grid.length;
        int height = grid[0].length;
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] != -1) continue;
                
                int minHeight = Integer.MAX_VALUE;
                for (int d = 0; d < 4; d++) {
                    int nx = x + DX[d];
                    int ny = y + DY[d];
                    
                    if (isValid(nx, ny, width, height) && grid[nx][ny] != -1) {
                        if (grid[nx][ny] < minHeight) {
                            minHeight = grid[nx][ny];
                        }
                    }
                }
                
                if (minHeight != Integer.MAX_VALUE) {
                    int newHeight = minHeight + 1;
                    grid[x][y] = newHeight;
                    stats.put(newHeight, stats.getOrDefault(newHeight, 0) + 1);
                } else {
                    grid[x][y] = 0; // На крайний случай
                }
            }
        }
    }

    private boolean isValid(int x, int y, int w, int h) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    @Override
    public String getName() {
        return "wave_noise";
    }

    static class Coord {
        final int x, y, height;
        final double noise;

        Coord(int x, int y, int height, double noise) {
            this.x = x;
            this.y = y;
            this.height = height;
            this.noise = noise;
        }
    }

    // Упрощенная реализация шума Перлина
    static class PerlinNoise {
        private final int[] permutations = new int[512];
        
        public PerlinNoise(int seed) {
            Random rand = new Random(seed);
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) p[i] = i;
            for (int i = 0; i < 256; i++) {
                int j = rand.nextInt(256 - i) + i;
                int temp = p[i];
                p[i] = p[j];
                p[j] = temp;
            }
            System.arraycopy(p, 0, permutations, 0, 256);
            System.arraycopy(p, 0, permutations, 256, 256);
        }
        
        public double perlin(double x, double y) {
            // Быстрая реализация 2D шума
            int X = (int)Math.floor(x) & 255;
            int Y = (int)Math.floor(y) & 255;
            
            x -= Math.floor(x);
            y -= Math.floor(y);
            
            double u = fade(x);
            double v = fade(y);
            
            int a = permutations[X] + Y;
            int aa = permutations[a];
            int ab = permutations[a + 1];
            int b = permutations[X + 1] + Y;
            int ba = permutations[b];
            int bb = permutations[b + 1];
            
            double lerp1 = lerp(u, grad(aa, x, y), grad(ba, x-1, y));
            double lerp2 = lerp(u, grad(ab, x, y-1), grad(bb, x-1, y-1));
            return lerp(v, lerp1, lerp2);
        }
        
        private double fade(double t) { 
            return t * t * t * (t * (t * 6 - 15) + 10);
        }
        
        private double lerp(double t, double a, double b) { 
            return a + t * (b - a); 
        }
        
        private double grad(int hash, double x, double y) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 4 ? y : (h == 12 || h == 14) ? x : 0;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }
}