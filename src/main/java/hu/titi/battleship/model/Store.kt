package hu.titi.battleship.model

private const val TAG = "store"

class Store<T> {

    private val lock = java.lang.Object()
    private val storage = mutableListOf<T>()

    fun place(elem: T) {
        synchronized(lock) {
            if (storage.isEmpty()) {
                storage.add(0, elem)
                lock.notify()
            }
        }
    }

    fun await(): T {
        synchronized(lock) {
            while (storage.isEmpty()) {
                lock.wait()
            }
            return storage.removeAt(0)
        }
    }

    fun visit(): T {
        synchronized(lock) {
            while (storage.isEmpty()) {
                lock.wait()
            }
            return storage[0]
        }
    }

    fun remove() {
        synchronized(lock) {
            if (storage.isNotEmpty()) {
                storage.removeAt(0)
                lock.notify()
            }
        }
    }

    fun set(elem: T) {
        synchronized(lock) {
            storage.clear()
            storage.add(elem)
            lock.notify()
        }
    }

    operator fun <R> invoke(code: T.() -> R): R {
        synchronized(lock) {
            while (storage.isEmpty()) {
                lock.wait()
            }
            return storage[0].code()
        }
    }

    fun visitIfPresent(): T? {
        synchronized(lock) {
            return if (storage.isEmpty()) {
                null
            } else {
                storage[0]
            }
        }
    }

}