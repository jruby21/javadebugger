(require 'dash)

(add-to-list 'load-path ".")

(require 'jbug)

(defun jbugTest ()
  (interactive)

  (start-process "testProc" "*testProc*" "java" "-cp" "/home/jruby/dev/javadebugger/src/main/java" "-agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y" "test.foo" "3" "4")

  (load-file "/home/jruby/dev/javadebugger/src/main/elisp/jbug.el")
  (load-file "/home/jruby/dev/javadebugger/src/main/elisp/test.el")

  (add-hook
   `jbug-mode-hook
   `jbug-TestWaitForInitialisation)

  (setq jbug-proxy-command "java -cp /home/jruby/dev/javadebugger/src/main/java/jbug.jar com.github.jruby21.javadebugger.JavaDebuggerProxy")0
  (jbug "/home/jruby/dev/javadebugger/src/main/java" "test.foo" "127.0.0.1" "8000"))

(defun jbug-TestWaitForInitialisation ()
    (jbug-addResponseTable
   "setup test script"
   (ht
    (jbug-breakpointEntered-response
     `(lambda (env resp)
       (let
           ((lm (locationMethod (-slice resp 8)))
            (ln (locationLineNumber (-slice resp 8)))
            (jbug-testThread  (threadID (-slice resp 2 8))))
         (jbug-addResponseCommand
          (cond
           ((and (string= lm "main") (not (string= ln "44")))
            (format "breaks;clear all;breaks;break test.foo 44;break test.foo sum;next %s;next %s;next %s;locals * %s 0;continue"
                    jbug-testThread jbug-testThread jbug-testThread  jbug-testThread))
           ((and (string= lm "main") (string= ln "44"))
            (format "locals * %s 0;stack %s;next %s;next %s;into %s;next %s;next %s;next %s;next %s; next %s;classes;fields test.tree.Node;stack %s;back %s;stack %s;continue"
                    jbug-testThread jbug-testThread jbug-testThread  jbug-testThread  jbug-testThread  jbug-testThread  jbug-testThread

                    jbug-testThread jbug-testThread jbug-testThread  jbug-testThread  jbug-testThread
                    jbug-testThread))
           ((string= lm "sum")
            (format "threads;arguments * %s;arguments f %s;arguments f.a  %s;arguments arr.1 %s;arguments arr.5-60 %s;arguments arr.58 %s;this"
                    jbug-testThread jbug-testThread jbug-testThread  jbug-testThread  jbug-testThread
                    jbug-testThread)))))))

    (jbug-accessWatchpoint-response
     `(lambda (env  resp)
       ()))

    (jbug-modificationWatchpoint-response
     `(lambda (env  resp)
       ())))))
