package gr.imsi.athenarc.xtremexpvisapi.domain.experiment;

public class UserEvaluation {
    // The identifier of the user submitting the evaluation
    private String user;

    // A numerical score for the run (e.g., 1–5) - optional
    private Integer rating;

    // Indicates whether the user marked this run as a favorite - optional
    private Boolean favorite;

    // Optional free-text comment describing the user’s evaluation
    private String comment;

    public UserEvaluation() {
    }

    /**
     * Constructor to initialize a UserEvaluation instance.
     *
     * @param user     The identifier of the user submitting the evaluation.
     * @param rating   A numerical score for the run (can be null).
     * @param favorite Whether the user marked this run as a favorite (can be null).
     * @param comment  Optional comment describing the evaluation (can be null).
     */
    public UserEvaluation(String user, Integer rating, Boolean favorite, String comment) {
        this.user = user;
        this.rating = rating;
        this.favorite = favorite;
        this.comment = comment;
    }

    // Getter and Setter methods
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns a string representation of the UserEvaluation instance.
     *
     * @return A formatted string containing user evaluation details.
     */
    @Override
    public String toString() {
        return "UserEvaluation{" +
                "user='" + user + '\'' +
                ", rating=" + rating +
                ", favorite=" + favorite +
                ", comment='" + comment + '\'' +
                '}';
    }
}
