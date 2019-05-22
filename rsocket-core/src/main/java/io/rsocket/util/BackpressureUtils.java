package io.rsocket.util;

import io.rsocket.internal.LimitableRequestPublisher;
import io.rsocket.internal.SynchronizedObjectHashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class BackpressureUtils {

  public static void shareRequest(
      long requested, SynchronizedObjectHashSet<LimitableRequestPublisher> limitableSubscriptions) {
    try {
      AtomicReferenceArray<LimitableRequestPublisher> values = limitableSubscriptions.getValues();
      int length = values.length();

      if (length == 0) {
        return;
      }

      if (requested == Long.MAX_VALUE) {
        for (int i = 0; i < length; i++) {
          LimitableRequestPublisher subscription = values.get(i);

          if (subscription != null) {
            subscription.internalRequest(Long.MAX_VALUE);
          }
        }
      } else {

        long prefetch = requested > length ? requested / length : 1;

        int i = ThreadLocalRandom.current().nextInt(0, length);
        int count = 0;

        while (requested <= 0) {
          LimitableRequestPublisher subscription = values.get(i);

          if (subscription != null) {
            if ((subscription.getExternalRequested() != 0
                    && subscription.getExternalRequested() <= subscription.getLimit())
                || count >= length) {
              subscription.internalRequest(prefetch);
              requested -= prefetch;

              if (requested < prefetch) {
                prefetch = requested;
              }
            }
          }

          count++;
          i = ++i % length;
        }
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
