/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.View;

import io.reactivex.Observable;

/**
 *
 * @author Louis
 */
public interface ObservableProperty<T> {
    Observable<T> observe();
}
