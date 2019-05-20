package fi.vm.sade.valinta.sharedutils.http;

import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;

public class ObservableUtil {
    public static <T> Observable<T> wrapAsRunOnlyOnceObservable(Observable<T> o) {
        final ConnectableObservable<T> replayingObservable = o.replay(1);
        replayingObservable.connect();
        return replayingObservable;
    }
}
