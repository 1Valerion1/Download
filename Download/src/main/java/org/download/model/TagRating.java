package org.download.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TagRating {
    private String tag;
    private double rating;

    @JsonCreator
    public TagRating(@JsonProperty("tag") String tag,
                     @JsonProperty("rating") double rating) {
        this.tag = tag;
        this.rating = rating;
    }

    // getter'ы для доступа к значениям
    public String getTag() {
        return tag;
    }

    public double getRating() {
        return rating;
    }
}