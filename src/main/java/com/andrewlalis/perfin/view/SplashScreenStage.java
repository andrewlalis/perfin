package com.andrewlalis.perfin.view;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SplashScreenStage extends Stage {
    public enum State {
        LOADING,
        DONE,
        ERROR
    }

    private final SimpleObjectProperty<State> stateProperty = new SimpleObjectProperty<>(State.LOADING);

    public SplashScreenStage(String title, Scene scene) {
        setTitle(title);
        setResizable(false);
        initStyle(StageStyle.UNDECORATED);
        setScene(scene);
    }

    public void setDone() {
        stateProperty.set(State.DONE);
        close();
    }

    public void setError() {
        stateProperty.set(State.ERROR);
        close();
    }

    public ObservableValue<State> stateProperty() {
        return this.stateProperty;
    }
}
