package org.xet.experiments.builder.algorithm.height_map;

import java.util.*;
import java.util.stream.Collectors;

public class GeneratorGame extends GeneratorHeightMap {
    @Override
    public int[][] getHeightMap(int width, int height) {
        int[][] heightMap = new int[width][height];

        GameGenerator generator = new GameGenerator(width, height);
        generator.init();
        while (generator.tick()) {
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                heightMap[x][y] = generator.getCoordinate(new Coord(x, y));
            }
        }

        return heightMap;
    }

    @Override
    public String getName() {
        return "game";
    }


    class Constants {
        public static final int BRUSH_STRENGTH = 3;
        public static final int NUM_POINTS_FOR_DIRECTION = 10;
        public static final double VELOCITY_MULTIPLIER = 0.1;
        public static final int VELOCITY_TIME_STEP = 1;
        public static final int VELOCITY_DISTANCE_TO_TARGET_FOR_SLOW_SPEED = 8;
    }

    static class Coord {
        public final int x;
        public final int y;

        public Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Coord add(Coord other) {
            return new Coord(this.x + other.x, this.y + other.y);
        }

        public Coord mul(int other) {
            return new Coord(this.x * other, this.y * other);
        }

        public Coord mul(Coord other) {
            return new Coord(this.x * other.x, this.y * other.y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Coord)) return false;
            Coord coord = (Coord) o;
            return x == coord.x && y == coord.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    static class DecimalCoordinate {
        public final double x;
        public final double y;

        public DecimalCoordinate(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Coord toCoordinate() {
            return new Coord((int) Math.round(x), (int) Math.round(y));
        }

        public DecimalCoordinate add(DecimalCoordinate other) {
            return new DecimalCoordinate(this.x + other.x, this.y + other.y);
        }

        public DecimalCoordinate sub(DecimalCoordinate other) {
            return new DecimalCoordinate(this.x - other.x, this.y - other.y);
        }

        public DecimalCoordinate mul(double other) {
            return new DecimalCoordinate(this.x * other, this.y * other);
        }

        public DecimalCoordinate div(double other) {
            return new DecimalCoordinate(this.x / other, this.y / other);
        }

        public double length() {
            return Math.sqrt(x * x + y * y);
        }

        public DecimalCoordinate normalize() {
            double length = length();
            if (length == 0) {
                return new DecimalCoordinate(0, 0);
            }
            return this.div(length);
        }
    }

    static class Brush {
        private final int strength;

        public Brush(int strength) {
            this.strength = strength;
        }

        public List<Map.Entry<Coord, Coord>> getStrengthMask() {
            List<Map.Entry<Coord, Coord>> mask = new ArrayList<>();
            for (int curStrength = 0; curStrength < strength; curStrength++) {
                for (int hStrength = 0; hStrength < strength; hStrength++) {
                    if (curStrength - hStrength < 0) break;
                    if (curStrength == 0 && hStrength == 0) continue;

                    Coord coordinate = new Coord(curStrength - hStrength, hStrength);
                    List<Coord> maybesPrev = new ArrayList<>();
                    if (coordinate.x - 1 >= 0) maybesPrev.add(new Coord(coordinate.x - 1, coordinate.y));
                    if (coordinate.y - 1 >= 0) maybesPrev.add(new Coord(coordinate.x, coordinate.y - 1));

                    if (maybesPrev.isEmpty()) {
                        throw new IllegalArgumentException("No previous coordinates");
                    }

                    Coord prevCoordinate = maybesPrev.get(new Random().nextInt(maybesPrev.size()));
                    mask.add(Map.entry(prevCoordinate, coordinate));
                }
            }

            // Adding mirrored coordinates
            for (Coord mul : Arrays.asList(new Coord(-1, 1), new Coord(1, -1), new Coord(-1, -1))) {
                List<Map.Entry<Coord, Coord>> newMask = new ArrayList<>();
                for (Map.Entry<Coord, Coord> entry : mask) {
                    newMask.add(Map.entry(entry.getKey().mul(mul), entry.getValue().mul(mul)));
                }
                mask.addAll(newMask);
            }

            return mask.stream().distinct().collect(Collectors.toList());
        }
    }

    static class Filler {
        private final GameGenerator generator;
        private final Brush brush;
        private DecimalCoordinate position;
        private Coord toCoordinate;
        private DecimalCoordinate velocity;

        public Filler(GameGenerator generator, Coord coordinate) {
            this.generator = generator;
            this.brush = new Brush(Constants.BRUSH_STRENGTH);
            this.position = new DecimalCoordinate(coordinate.x, coordinate.y);
            this.toCoordinate = position.toCoordinate();
            this.velocity = new DecimalCoordinate(new Random().nextDouble() * 2 - 1, new Random().nextDouble() * 2 - 1);
        }

        public void init(boolean firstTick) {
            if (firstTick) {
                generator.setCoordinate(position.toCoordinate(), 0);
            }
            fill();
            chooseNewDirection();
        }

        public void tick() {
            position = position.add(velocity);

            if (position.x < 0) {
                position = new DecimalCoordinate(0, position.y);
                velocity = new DecimalCoordinate(-velocity.x, velocity.y);
            } else if (position.x >= generator.width) {
                position = new DecimalCoordinate(generator.width - 1, position.y);
                velocity = new DecimalCoordinate(-velocity.x, velocity.y);
            }

            if (position.y < 0) {
                position = new DecimalCoordinate(position.x, 0);
                velocity = new DecimalCoordinate(velocity.x, -velocity.y);
            } else if (position.y >= generator.height) {
                position = new DecimalCoordinate(position.x, generator.height - 1);
                velocity = new DecimalCoordinate(velocity.x, -velocity.y);
            }

            fill();

            if (position.toCoordinate().equals(toCoordinate)) {
                chooseNewDirection();
            }

            DecimalCoordinate toPosition = new DecimalCoordinate(toCoordinate.x, toCoordinate.y);
            DecimalCoordinate direction = toPosition.sub(position).normalize();
            double distanceToTarget = toPosition.sub(position).length();

            if (distanceToTarget < velocity.length() * Constants.VELOCITY_DISTANCE_TO_TARGET_FOR_SLOW_SPEED) {
                velocity = direction.mul(distanceToTarget);
            } else {
                velocity = velocity.add(direction.mul(Constants.VELOCITY_MULTIPLIER));
            }

            double maxSpeed = 0.7;
            if (velocity.length() > maxSpeed) {
                velocity = velocity.normalize().mul(maxSpeed);
            }
        }

        public void chooseNewDirection() {
            List<Coord> points = new ArrayList<>();
            int availablePoints = 0;

            for (int x = 0; x < generator.width; x++) {
                for (int y = 0; y < generator.height; y++) {
                    if (generator.getCoordinate(new Coord(x, y)) == null) {
                        availablePoints++;
                    }
                }
            }

            if (availablePoints == 0) {
                toCoordinate = position.toCoordinate();
                return;
            }

            Random random = new Random();
            while (points.size() < Math.min(availablePoints, Constants.NUM_POINTS_FOR_DIRECTION)) {
                int x = random.nextInt(generator.width);
                int y = random.nextInt(generator.height);
                Coord point = new Coord(x, y);
                if (point.equals(position.toCoordinate())) continue;
                if (generator.getCoordinate(point) != null) continue;
                points.add(point);
            }

            toCoordinate = points.stream().max(Comparator.comparingDouble(this::getWeightOfPoint)).orElse(points.get(0));
        }

        public double getWeightOfPoint(Coord point) {
            List<Coord> coordinates = generator.getCoordinatesFromMask(point, brush.getStrengthMask());
            double value = coordinates.stream().filter(coord -> generator.getCoordinate(coord) == null).count();
            value *= 1.5 * (generator.getCoordinate(point) == null ? 1 : 0);
            value += 1;
            return value;
        }

        public void fill() {
            generator.fillMask(position.toCoordinate(), brush.getStrengthMask());
        }
    }

    static class GameGenerator {
        public final int width;
        public final int height;
        private final int[][] map;
        private final List<Filler> fillers = new ArrayList<>();
        public boolean isInited = false;

        public GameGenerator(int width, int height) {
            this.width = width;
            this.height = height;
            this.map = new int[width][height];
        }

        public void init() {
            spawnFiller(new Coord(new Random().nextInt(width), new Random().nextInt(height)));
            isInited = true;
        }

        public boolean tick() {
            if (checkMapIsComplete()) return false;

            List<Filler> fillersCopy = new ArrayList<>(fillers);
            for (Filler filler : fillersCopy) {
                if (needSpawnFiller(filler)) {
                    spawnFiller(filler.position.toCoordinate());
                }
            }

            fillers.forEach(Filler::tick);

            return true;
        }

        public boolean needSpawnFiller(Filler filler) {
            return 0.05 * (1 / (getFillingMap() / 10.0) / 1000.0) * ((width * height) / 100.0) / Math.pow(fillers.size(), 2) > new Random().nextDouble();
        }

        public double getFillingMap() {
            return Arrays.stream(map).flatMapToInt(Arrays::stream).filter(value -> value != 0).count() / (double) (width * height);
        }

        public Integer getCoordinate(Coord coordinate) {
            if (coordinate.x < 0 || coordinate.x >= width || coordinate.y < 0 || coordinate.y >= height) return null;
            if (map[coordinate.x][coordinate.y] == 0) return null;
            return map[coordinate.x][coordinate.y] - 1;
        }

        public void setCoordinate(Coord coordinate, Integer value) {
            if (coordinate.x < 0 || coordinate.x >= width || coordinate.y < 0 || coordinate.y >= height) return;
            map[coordinate.x][coordinate.y] = value + 1;
        }

        public void fillMask(Coord absCoordinate, List<Map.Entry<Coord, Coord>> mask) {
            for (Map.Entry<Coord, Coord> entry : mask) {
                Coord prevCoordinate = absCoordinate.add(entry.getKey());
                Coord coordinate = absCoordinate.add(entry.getValue());
                Integer prevValue = getCoordinate(prevCoordinate);
                Integer curValue = getCoordinate(coordinate);

                if (prevValue == null || curValue != null) continue;

                setCoordinate(coordinate, prevValue + 1);
            }
        }

        public static List<Coord> getCoordinatesFromMask(Coord absCoordinate, List<Map.Entry<Coord, Coord>> mask) {
            return mask.stream().map(entry -> absCoordinate.add(entry.getValue())).collect(Collectors.toList());
        }

        public void spawnFiller(Coord spawnCoordinate) {
            Filler filler = new Filler(this, spawnCoordinate);
            filler.init(!isInited);
            fillers.add(filler);
        }

        public boolean checkMapIsComplete() {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (getCoordinate(new Coord(x, y)) == null) {
                        return false;
                    }
                }
            }
            return true;
        }

        public void printMap() {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Integer value = getCoordinate(new Coord(x, y));
                    System.out.print(value != null ? String.format("%2d ", value) : "   ");
                }
                System.out.println();
            }
        }
    }

}
