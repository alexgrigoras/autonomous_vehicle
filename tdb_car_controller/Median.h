#pragma once

template <typename T, uint8_t N, T default_value>
class Median {

  private:

    uint8_t _buffer_ptr;        // Pointer to the last added element in _inbuffer
    uint8_t _window_size;       // sliding window size
    uint8_t _median_ptr;        // position of the median value in _sortbuffer

    T _inbuffer[N];             // cyclic buffer for incoming values
    T _sortbuffer[N];           // sorted buffer

  public:

    // constructor
    Median() {
      _buffer_ptr = N;
      _window_size = N;
      _median_ptr = N / 2;

      // Initialize buffers
      uint8_t i = _window_size;
      while ( i > 0 ) {
        i--;
        _inbuffer[i] = default_value;
        _sortbuffer[i] = default_value;
      }
    }

    // median value
    T getMedian() {
      // buffers are always sorted.
      return _sortbuffer[_median_ptr];
    }

    // add new value
    void addValue(T new_value) {

      // comparision with 0 is fast, so we decrement _buffer_ptr
      if (_buffer_ptr == 0)
        _buffer_ptr = _window_size;

      _buffer_ptr--;

      T old_value = _inbuffer[_buffer_ptr];     // retrieve the old value to be replaced
      if (new_value == old_value)               // if the value is unchanged, do nothing
        return;

      _inbuffer[_buffer_ptr] = new_value;       // fill the new value in the cyclic buffer

      // search the old_value in the sorted buffer
      uint8_t i = _window_size;
      while (i > 0) {
        i--;
        if (old_value == _sortbuffer[i])
          break;
      }

      // i is the index of the old_value in the sorted buffer
      _sortbuffer[i] = new_value;               // replace the value

      // the sortbuffer is always sorted, except the [i]-element..
      if (new_value > old_value) {
        //  if the new value is bigger than the old one, make a bubble sort upwards
        for (uint8_t p = i, q = i + 1; q < _window_size; p++, q++) {
          // bubble sort step
          if (_sortbuffer[p] > _sortbuffer[q]) {
            T tmp = _sortbuffer[p];
            _sortbuffer[p] = _sortbuffer[q];
            _sortbuffer[q] = tmp;
          } else {
            // done ! - found the right place
            return;
          }
        }
      } else {
        // else new_value is smaller than the old one, bubble downwards
        for (int p = i - 1, q = i; q > 0; p--, q--) {
          if (_sortbuffer[p] > _sortbuffer[q]) {
            T tmp = _sortbuffer[p];
            _sortbuffer[p] = _sortbuffer[q];
            _sortbuffer[q] = tmp;
          } else {
            // done !
            return;
          }
        }
      }

    }

};

