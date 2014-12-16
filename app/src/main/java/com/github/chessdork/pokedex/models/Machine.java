package com.github.chessdork.pokedex.models;



public class Machine {
    private final String name, move, location;

    public Machine(String name, String move, String location) {
        this.name = name;
        this.move = move;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public String getMove() {
        return move;
    }

    public String getLocation() {
        return location;
    }

    public String toString() {
        return name;
    }

}
