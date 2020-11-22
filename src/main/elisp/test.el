(require 'dash)

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

(defvar jbug-testThread 0)

(defvar jbug-test-steps ())

(setq jbug-test-steps
      (list
       (list
        jbug-breakpointEntered-response  "main"  ""
        `(format "next %s;next %s;break test.foo sum;next %s;locals * %s 0;break test.foo 44"
                 jbug-testThread jbug-testThread jbug-testThread jbug-testThread))
       (list
        jbug-breakpointEntered-response "" "44"
        `(format "locals * %s 0;stack %s;next %s;next %s;into %s;next %s;next %s;next %s;classes;stack %s;back %s;stack %s;access test.foo$XThread first;modify test.foo$XThread second"
                 jbug-testThread
                 jbug-testThread
                 jbug-testThread
                 jbug-testThread
                 jbug-testThread
                 jbug-testThread
                 jbug-testThread
                jbug-testThread
                jbug-testThread
                jbug-testThread
                jbug-testThread))
       (list
        jbug-breakpointEntered-response "sum" ""
        `(format "threads;arguments %s;arguments f %s;arguments f.a  %s;arguments arr.1 %s;arguments arr.5-60 %s;arguments arr.58 %s;this;"
                  jbug-testThread
                  jbug-testThread
                  jbug-testThread
                  jbug-testThread
                  jbug-testThread
                  jbug-testThread))
        (list
         jbug-accessWatchpoint-response
         ())
        (list
         jbug-modificationWatchpoint-response
        ())))

(defun jbug-TestWaitForInitialisation ()
    (jbug-addResponseTable
   "setup test script"
   (ht
    (jbug-breakpointEntered-response
     `jbug-TestBreakpointEntered)
    (jbug-accessWatchpoint-response
     `jbug-TestBreakpointEntered)
    (jbug-modificationWatchpoint-response
    `jbug-TestBreakpointEntered))))

(defun jbug-TestBreakpointEntered (env resp)
  (setq jbug-testThread  (threadID (-slice resp 2 8)))
  (dolist (step jbug-test-steps)
    (when
        (and
         (string= (car step) (car resp))
         (or
          (string= jbug-accessWatchpoint-response (car resp))
          (string= jbug-modificationWatchpoint-response (car resp))
          (string= (nth 1 step)   (locationMethod (-slice resp 8)))
          (string= (nth 2 step)   (locationLineNumber (-slice resp 8)))))
          (progn
            (setq jbug-test-steps (-remove-item step jbug-test-steps))
            (setq jbug-responseCommands
                  (cons
                   (concat
                    (if (nth 3 step) (eval (nth 3 step)) "")
                    (if jbug-test-steps ";continue" ""))
                   jbug-responseCommands))))))
