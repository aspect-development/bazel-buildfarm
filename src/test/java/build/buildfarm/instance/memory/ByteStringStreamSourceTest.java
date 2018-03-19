// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buildfarm.instance.memory;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.IOException;
import java.io.InputStream;

@RunWith(JUnit4.class)
public class ByteStringStreamSourceTest {
  @Test
  public void closeCallsOnCloseOnce() throws IOException {
    Runnable mockOnClose = mock(Runnable.class);
    ByteStringStreamSource source = new ByteStringStreamSource(mockOnClose);
    verify(mockOnClose, never()).run();
    source.getOutputStream().close();
    verify(mockOnClose, times(1)).run();
  }

  @Test
  public void closeShouldSetIsClosed() throws IOException {
    ByteStringStreamSource source = new ByteStringStreamSource(() -> {});
    assertThat(source.isClosed()).isFalse();
    source.getOutputStream().close();
    assertThat(source.isClosed()).isTrue();
  }

  @Test
  public void openStreamCanStreamUpdates() throws IOException {
    ByteStringStreamSource source = new ByteStringStreamSource(() -> {});
    InputStream inputStream = source.openStream();
    assertThat(inputStream.available()).isEqualTo(0);
    source.getOutputStream().write('a');
    assertThat(inputStream.available()).isEqualTo(1);
    assertThat(inputStream.read()).isEqualTo('a');
  }

  @Test
  public void readAtClosedEnd() throws IOException {
    ByteStringStreamSource source = new ByteStringStreamSource(() -> {});
    InputStream inputStream = source.openStream();
    source.getOutputStream().close();
    assertThat(inputStream.read()).isEqualTo(-1);
  }


  @Test
  public void skipNegativePreventsSkip() throws IOException {
    ByteStringStreamSource source = new ByteStringStreamSource(() -> {});
    InputStream inputStream = source.openStream();
    assertThat(inputStream.skip(-1)).isEqualTo(0);
  }

  @Test
  public void skipBlocksForInput() throws IOException, InterruptedException {
    ByteStringStreamSource source = new ByteStringStreamSource(() -> {});
    InputStream inputStream = source.openStream();
    Thread thread = new Thread(() -> {
      try {
        assertThat(inputStream.skip(1)).isEqualTo(1);
      } catch (IOException e) {
        fail("Unexpected IOException: " + e);
      }
    });
    thread.start();
    while (!thread.isAlive()) {
      Thread.sleep(10);
    }
    source.getOutputStream().write('a');
    while (thread.isAlive()) {
      Thread.sleep(10);
    }
  }

  @Test
  public void readBlocksForInput() throws IOException, InterruptedException {
    ByteStringStreamSource source = new ByteStringStreamSource(() -> {});
    InputStream inputStream = source.openStream();
    Thread thread = new Thread(() -> {
      try {
        assertThat(inputStream.read()).isEqualTo('a');
      } catch (IOException e) {
        fail("Unexpected IOException: " + e);
      }
    });
    thread.start();
    while (!thread.isAlive()) {
      Thread.sleep(10);
    }
    source.getOutputStream().write('a');
    while (thread.isAlive()) {
      Thread.sleep(10);
    }
  }

  @Test
  public void readZeroBytesIgnoresBuffer() throws IOException {
    ByteStringStreamSource source = new ByteStringStreamSource(() -> {});
    InputStream inputStream = source.openStream();
    assertThat(inputStream.read(null, -1, 0)).isEqualTo(0);
  }
}
