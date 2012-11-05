/*
 *  Copyright 2011-2012 Mathieu ANCELIN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package devoxx.core.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

/**
 * Utilities for everyday stuff functional style.
 * 
 * Highly inspired by : https://github.com/playframework/play/blob/master/framework/src/play/libs/F.java
 *
 * @author Mathieu ANCELIN
 */
public final class F {

    final static None<Object> none = new None<Object>();

    private F() {}

    public static class Unit {
        private static final Unit instance = new Unit();
        private Unit() {}
        public static Unit unit() { return instance; }
    }

    public static interface Callable<T> {

        T apply();
    }
    
    public static interface CheckedCallable<T> {

        T apply() throws Throwable;
    }

    public static interface SimpleCallable extends Callable<Unit> {}
    
    public static interface SimpleCheckedCallable extends CheckedCallable<Unit> {}

    public static interface Action<T> {

        void apply(T t);
    }
    
    public static interface CheckedAction<T> {

        void apply(T t) throws Throwable;
    }

    public static interface Function<T, R> {

        R apply(T t);
    }
        
    public static interface CheckedFunction<T, R> {

        R apply(T t) throws Throwable;
    }

    public static interface Monad<T> {

        <R> Option<R> map(Function<T, R> function);

        Option<T> map(Action<T> function);
        
        Option<T> map(Callable<T> function);

        <R> Option<R> map(CheckedFunction<T, R> function);

        Option<T> map(CheckedAction<T> function);
        
        Option<T> map(CheckedCallable<T> function);
        
        Option<T> flatMap(Callable<Option<T>> action);

        Option<T> flatMap(CheckedCallable<Option<T>> action);

        Option<T> flatMap(Function<T, Option<T>> action);

        Option<T> flatMap(CheckedFunction<T, Option<T>> action);

        /**Option<T> bind(Action<Option<T>> action);

        Option<T> bind(CheckedAction<Option<T>> action);**/
    }

    public static abstract class Option<T> implements Iterable<T>, Monad<T>, Serializable {
        
        public abstract boolean isDefined();

        public abstract boolean isEmpty();
        
        public abstract T get();

        public Option<T> orElse(T value) {
            return isEmpty() ? Option.maybe(value) : this;
        }
        
        public T getOrElse(T value) {
            return isEmpty() ? value : get();
        }

        public T getOrElse(Function<Unit, T> function) {
            return isEmpty() ? function.apply(Unit.unit()) : get();
        }

        public T getOrElse(Callable<T> function) {
            return isEmpty() ? function.apply() : get();
        }

        public T getOrNull() {
            return isEmpty() ? null : get();
        }
        
        public Option<T> filter(Function<T, Boolean> predicate) {
            if (isDefined()) {
                if (predicate.apply(get())) {
                    return this;
                } else {
                    return Option.none();
                }
            }
            return Option.none();
        }
        
        public Option<T> filterNot(Function<T, Boolean> predicate) {
            if (isDefined()) {
                if (!predicate.apply(get())) {
                    return this;
                } else {
                    return Option.none();
                }
            }
            return Option.none();
        }

        public <X> Either<X, T> toRight(X left) {
            if (isDefined()) {
                return Either.eitherRight(get());
            } else {
                return Either.eitherLeft(left);
            }
        }

        public <X> Either<T, X> toLeft(X right) {
             if (isDefined()) {
                return Either.eitherLeft(get());
            } else {
                return Either.eitherRight(right);
            }
        }

        @Override
        public <R> Option<R> map(Function<T, R> function) {
            if (isDefined()) {
                return Option.maybe(function.apply(get()));
            }
            return Option.none();
        }

        @Override
        public Option<T> map(Callable<T> function) {
            if (isDefined()) {
                return Option.maybe(function.apply());
                //return Option.maybe(get());
            }
            return Option.none();
        }
        
        @Override
        public Option<T> map(Action<T> function) {
            if (isDefined()) {
                function.apply(get());
                return Option.maybe(get());
            }
            return Option.none();
        }

        @Override
        public <R> Option<R> map(CheckedFunction<T, R> function) {
            if (isDefined()) {
                try {
                    return Option.maybe(function.apply(get()));
                } catch (Throwable t) {
                    return Option.none();
                }
            }
            return Option.none();
        }

        @Override
        public Option<T> map(CheckedCallable<T> function) {
            if (isDefined()) {
                try {
                    return Option.maybe(function.apply());
                    //return Option.maybe(get());
                } catch (Throwable t) {
                    return Option.none();
                }
            }
            return Option.none();
        }
        
        @Override
        public Option<T> map(CheckedAction<T> function) {
            if (isDefined()) {
                try {
                    function.apply(get());
                    return Option.maybe(get());
                } catch (Throwable t) {
                    return Option.none();
                }
            }
            return Option.none();
        }

        /**@Override
        public Option<T> bind(Action<Option<T>> action) {
            if (isDefined()) {
                action.apply(this);
                return this;
            }
            return Option.none();
        }

        @Override
        public Option<T> bind(CheckedAction<Option<T>> action) {
           if (isDefined()) {
                try {
                    action.apply(this);
                    return this;
                } catch (Throwable t) {
                    return this;
                }
            }
            return Option.none();
        }**/
        
        @Override
        public Option<T> flatMap(Callable<Option<T>> action) {
            if (isDefined()) {
                return action.apply();
            }
            return Option.none();
        }
        
        @Override
        public Option<T> flatMap(CheckedCallable<Option<T>> action) {
           if (isDefined()) {
                try {
                    return action.apply();
                } catch (Throwable t) {
                    return this;
                }
            }
            return Option.none();
        }
        
        @Override
        public Option<T> flatMap(Function<T, Option<T>> action) {
            if (isDefined()) {
                return action.apply(get());
            }
            return Option.none();
        }

        @Override
        public Option<T> flatMap(CheckedFunction<T, Option<T>> action) {
           if (isDefined()) {
                try {
                    return action.apply(get());
                } catch (Throwable t) {
                    return this;
                }
            }
            return Option.none();
        }

        public static <T> None<T> none() {
            return (None<T>) (Object) none;
        }

        public static <T> Some<T> some(T value) {
            return new Some<T>(value);
        }

        public static <T> Option<T> maybe(T value) {
            return apply(value);
        }
        
        public static <T> Option<T> apply(T value) {
            if (value == null) {
                return Option.none();
            } else {
                return Option.some(value);
            }
        }
        
        public static <T> Option<T> unit(T value) {
            return apply(value);
        }
    }

    public static class None<T> extends Option<T> {

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public T get() {
            throw new IllegalStateException("No value");
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public String toString() {
            return "None";
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    public static class Some<T> extends Option<T> {

        final T value;

        public Some(T value) {
            this.value = value;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public String toString() {
            return "Some ( " + value + " )";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    public static class Either<A, B> {

        final public Left<A, B> left;
        final public Right<B, A> right;

        private Either(A left, B right) {
            this.left = new Left<A, B>(left, this);
            this.right = new Right<B, A>(right, this);
        }

        public static <A, B> Either<A, B> eitherLeft(A value) {
            return new Either<A, B>(value, null);
        }

        public static <A, B> Either<A, B> eitherRight(B value) {
            return new Either<A, B>(null, value);
        }

        public <A, B> Either<A, B> left(A value) {
            if (value != null) {
                return new Either<A, B>(value, null);
            }
            return new Either(left, right);
        }

        public <A, B> Either<A, B> right(B value) {
            if (value != null) {
                return new Either<A, B>(null, value);
            }
            return new Either(left, right);
        }

        public <A, B> Either<A, B> left(Option<A> value) {
            if (value.isDefined()) {
                return new Either<A, B>(value.get(), null);
            }
            return new Either(left, right);
        }

        public <A, B> Either<A, B> right(Option<B> value) {
            if (value.isDefined()) {
                return new Either<A, B>(null, value.get());
            }
            return new Either(left, right);
        }

        public <X> Option<X> fold(Function<A, X> fa, Function<B, X> fb) {
            if (isLeft()) {
                return Option.maybe(fa.apply(left.get()));
            } else if (isRight()) {
                return Option.maybe(fb.apply(right.get()));
            } else {
                return (Option<X>) Option.none();
            }
        }

        public <X> Option<X> fold(Callable<X> fa, Callable<X> fb) {
            if (isLeft()) {
                return Option.maybe(fa.apply());
            } else if (isRight()) {
                return Option.maybe(fb.apply());
            } else {
                return (Option<X>) Option.none();
            }
        }

        public <X> Either<A, B> fold(Action<A> fa, Action<B> fb) {
            if (isLeft()) {
                fa.apply(left.get());
                return new Either<A, B>(left.get(), null);
            } else if (isRight()) {
                fb.apply(right.get());
                return new Either<A, B>(null, right.get());
            } else {
                return new Either<A, B>(null, null);
            }
        }

        public boolean isLeft() {
            return left.isDefined();
        }

        public boolean isRight() {
            return right.isDefined();
        }

        public Either<B, A> swap() {
            A vLeft = null;
            B vRight = null;
            if (left.isDefined()) {
                vLeft = left.get();
            }
            if (right.isDefined()) {
                vRight = right.get();
            }
            return new Either<B, A>(vRight, vLeft);
        }

        @Override
        public String toString() {
            return "Either ( left: " + left + ", right: " + right + " )";
        }
    }
    
    public static class Left<A, B> implements Iterable<A> {
        
        private final A input;
        
        public final Either<A, B> e;

        Left(A value, Either<A, B> e) {
            this.e = e;
            this.input = value;
        } 
        
        public boolean isDefined() {
            return !(input == null);
        }

        public A get() {
            return input;
        }

        @Override
        public Iterator<A> iterator() {
            if (input == null) {
                return Collections.<A>emptyList().iterator();
            } else {
                return Collections.singletonList(input).iterator();
            }
        }

        @Override
        public String toString() {
            return "Left ( " + input + " )";
        }

        public boolean isEmpty() {
            return !isDefined();
        }
        
        public A getOrElse(A value) {
            return isEmpty() ? value : get();
        }

        public A getOrElse(Function<Unit, A> function) {
            return isEmpty() ? function.apply(Unit.unit()) : get();
        }

        public A getOrElse(Callable<A> function) {
            return isEmpty() ? function.apply() : get();
        }

        public A getOrNull() {
            return isEmpty() ? null : get();
        }
        
        public Option<Either<A, B>> filter(Function<A, Boolean> predicate) {
            if (isDefined()) {
                if (predicate.apply(get())) {
                    return Option.maybe(e);
                } else {
                    return Option.none();
                }
            }
            return Option.none();
        }
        
        public Option<Either<A, B>> filterNot(Function<A, Boolean> predicate) {
            if (isDefined()) {
                if (!predicate.apply(get())) {
                    return Option.maybe(e);
                } else {
                    return Option.none();
                }
            }
            return Option.none();
        }

        public <R> Either<R, B> map(Function<A, R> function) {
            if (isDefined()) {
                return new Either<R, B>(function.apply(get()), null);
            } else {
                return new Either<R, B>(null, e.right.get());
            }
        }
        
        public <R> Either<R, B> flatMap(Callable<Either<R, B>> action) {
            if (isDefined()) {
                return action.apply();
            } else {
                return new Either<R, B>(null, e.right.get());
            }
        }
        
        public <R> Either<R, B> flatMap(Function<A, Either<R, B>> action) {
            if (isDefined()) {
                return action.apply(get());
            } else {
                return new Either<R, B>(null, e.right.get());
            }
        }
    }
    
    public static class Right<B, A> implements Iterable<B> {
        
        private final B input;
        
        public final Either<A, B> e;

        Right(B value, Either<A, B> e) {
            this.e = e;
            this.input = value;
        } 
        
        public boolean isDefined() {
            return !(input == null);
        }

        public B get() {
            return input;
        }

        @Override
        public Iterator<B> iterator() {
            if (input == null) {
                return Collections.<B>emptyList().iterator();
            } else {
                return Collections.singletonList(input).iterator();
            }
        }

        @Override
        public String toString() {
            return "Left ( " + input + " )";
        }

        public boolean isEmpty() {
            return !isDefined();
        }
        
        public B getOrElse(B value) {
            return isEmpty() ? value : get();
        }

        public B getOrElse(Function<Unit, B> function) {
            return isEmpty() ? function.apply(Unit.unit()) : get();
        }

        public B getOrElse(Callable<B> function) {
            return isEmpty() ? function.apply() : get();
        }

        public B getOrNull() {
            return isEmpty() ? null : get();
        }
        
        public Option<Either<A, B>> filter(Function<B, Boolean> predicate) {
            if (isDefined()) {
                if (predicate.apply(get())) {
                    return Option.maybe(e);
                } else {
                    return Option.none();
                }
            }
            return Option.none();
        }
        
        public Option<Either<A, B>> filterNot(Function<B, Boolean> predicate) {
            if (isDefined()) {
                if (!predicate.apply(get())) {
                    return Option.maybe(e);
                } else {
                    return Option.none();
                }
            }
            return Option.none();
        }

        public <R> Either<A, R> map(Function<B, R> function) {
            if (isDefined()) {
                return new Either<A, R>(null, function.apply(get()));
            } else {
                return new Either<A, R>(e.left.get(), null);
            }
        }
        
        public <R> Either<A, R> flatMap(Callable<Either<A, R>> action) {
            if (isDefined()) {
                return action.apply();
            } else {
                return new Either<A, R>(e.left.get(), null);
            }
        }
        
        public <R> Either<A, R> flatMap(Function<B, Either<A, R>> action) {
            if (isDefined()) {
                return action.apply(get());
            } else {
                return new Either<A, R>(e.left.get(), null);
            }
        }
    } 

    public static class Tuple<A, B> implements Serializable {

        final public A _1;
        final public B _2;

        public Tuple(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public Tuple<B, A> swap() {
            return new Tuple<B, A>(_2, _1);
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + " )";
        }
    }

    public static interface F2<A, B, R> {

        R apply(A a, B b);
    }

    public static interface F3<A, B, C, R> {

        R apply(A a, B b, C c);
    }

    public static interface F4<A, B, C, D, R> {

        R apply(A a, B b, C c, D d);
    }

    public static interface F5<A, B, C, D, E, R> {

        R apply(A a, B b, C c, D d, E e);
    }

    public static interface F6<A, B, C, D, E, F, R> {

        R apply(A a, B b, C c, D d, E e, F f);
    }

    public static interface F7<A, B, C, D, E, F, G, R> {

        R apply(A a, B b, C c, D d, E e, F f, G g);
    }

    public static interface F8<A, B, C, D, E, F, G, H, R> {

        R apply(A a, B b, C c, D d, E e, F f, G g, H h);
    }

    public static interface F9<A, B, C, D, E, F, G, H, I, R> {

        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i);
    }

    public static interface F10<A, B, C, D, E, F, G, H, I, J, R> {

        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
    }

    public static class Tuple3<A, B, C> {

        final public A _1;
        final public B _2;
        final public C _3;

        public Tuple3(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3 + " )";
        }
    }

    public static class Tuple4<A, B, C, D> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;

        public Tuple4(A _1, B _2, C _3, D _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: "
                + _3 + ", _4: " + _4 + " )";
        }
    }

    public static class Tuple5<A, B, C, D, E> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;

        public Tuple5(A _1, B _2, C _3, D _4, E _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + " )";
        }
    }

    public static class Tuple6<A, B, C, D, E, F> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;

        public Tuple6(A _1, B _2, C _3, D _4, E _5, F _6) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6 + " )";
        }
    }

    public static class Tuple7<A, B, C, D, E, F, G> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;
        final public G _7;

        public Tuple7(A _1, B _2, C _3, D _4, E _5, F _6, G _7) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6 + " )";
        }
    }

    public static class Tuple8<A, B, C, D, E, F, G, H> {
        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;
        final public G _7;
        final public H _8;

        public Tuple8(A _1, B _2, C _3, D _4, E _5, F _6, G _7, H _8) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6
                + ", _7: " + _7 + ", _8: " + _8
                + " )";
        }
    }

    public static class Tuple9<A, B, C, D, E, F, G, H, I> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;
        final public G _7;
        final public H _8;
        final public I _9;

        public Tuple9(A _1, B _2, C _3, D _4, E _5, F _6, G _7, H _8, I _9) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
            this._9 = _9;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6
                + ", _7: " + _7 + ", _8: " + _8 + ", _9: " + _9
                + " )";
        }
    }

    public static class Tuple10<A, B, C, D, E, F, G, H, I, J> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;
        final public G _7;
        final public H _8;
        final public I _9;
        final public J _10;

        public Tuple10(A _1, B _2, C _3, D _4, E _5, F _6, G _7, H _8, I _9, J _10) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
            this._9 = _9;
            this._10 = _10;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6
                + ", _7: " + _7 + ", _8: " + _8 + ", _9: " + _9
                + ", _10: " + _10 + " )";
        }
    }

    public static  class UncheckedAction<T> implements Action<T> {

        private final CheckedAction<T> action;

        public UncheckedAction(CheckedAction<T> action) {
            this.action = action;
        }

        @Override
        public void apply(T param) {
            try {
                action.apply(param);
            } catch (Throwable t) {
                throw new ExceptionWrapper(t);
            }
        }
    }

    public static class UncheckedFunction<T, R> implements Function<T, R> {

        private final CheckedFunction<T, R> function;

        public UncheckedFunction(CheckedFunction<T, R> function) {
            this.function = function;
        }

        @Override
        public R apply(T param) {
            try {
                return function.apply(param);
            } catch (Throwable ex) {
                throw new ExceptionWrapper(ex);
            }
        }
    }

    public static class ExceptionWrapper extends RuntimeException {

        private final Throwable t;

        public ExceptionWrapper(Throwable t) {
            this.t = t;
        }

        @Override
        public String getMessage() {
            return t.getMessage();
        }

        @Override
        public String getLocalizedMessage() {
            return t.getLocalizedMessage();
        }

        @Override
        public Throwable getCause() {
            return t.getCause();
        }

        @Override
        public synchronized Throwable initCause(Throwable throwable) {
            return t.initCause(throwable);
        }

        @Override
        public String toString() {
            return t.toString();
        }

        @Override
        public void printStackTrace() {
            t.printStackTrace();
        }

        @Override
        public void printStackTrace(PrintStream printStream) {
            t.printStackTrace(printStream);
        }

        @Override
        public void printStackTrace(PrintWriter printWriter) {
            t.printStackTrace(printWriter);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return t.fillInStackTrace();
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return t.getStackTrace();
        }

        @Override
        public void setStackTrace(StackTraceElement[] stackTraceElements) {
            t.setStackTrace(stackTraceElements);
        }
    }
}
