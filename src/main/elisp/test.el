(defun jbugTest ()
  (interactive)

  (start-process "testProc" "*testProc*" "/home/jruby/tools/jdk1.8.0_131/bin/java" "-cp" "/home/jruby/dev/javadebugger/src/main/java:/home/jruby/dev/javadebugger/src/main/java/tools.jar" "-agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y" "test.foo" "3" "4")

  (load-file "/home/jruby/dev/javadebugger/src/main/elisp/jbug.el")
  (load-file "/home/jruby/dev/javadebugger/src/main/elisp/test.el")

  (add-hook
   `jbug-mode-hook
   `jbug-TestWaitForInitialisation)

  (setq jbug-proxy-command "java -cp /home/jruby/dev/javadebugger/src/main/java/jbug.jar:/home/jruby/dev/javadebugger/src/main/java/tools.jar com.github.jruby21.javadebugger.JavaDebuggerProxy")
  (jbug "/home/jruby/dev/javadebugger/src/main/java" "test.foo" "127.0.0.1" "8000"))

(defun jbug-TestWaitForInitialisation ()
    (jbug-addResponseTable
   "setup test script"
   (ht
    (jbug-breakpointEntered-response
     `(lambda (env response)
        (when (string= "main"   (locationMethod (-slice response 8)))
          (jbug-TestJbug)
          (jbug-removeResponseTable env))))))
   (message "second running jbug-mode-hook to %s" (length  jbug-responseTables)))

(defun jbug-TestCommand (env response)
  (let ((script (ht-get env "SCRIPT")))
    (when (and script (string= (car response) (car (car script))))
        (jbug-add-commands
         (split-string (cdr (car script)) ";" 't))
        (setq script (cdr script)))
    (ht-set env "SCRIPT" script)
    (if (not script)
        (jbug-removeResponseTable env))))

(defun jbug-TestJbug ()
  (interactive)
  (message "running jbug-Testjbug %s" (length  jbug-responseTables))
  (jbug-addResponseTable
   "jbug-Testbug environment"
   (ht
    (jbug-breakpointEntered-response
     `jbug-TestCommand)
    (jbug-step-response
     `jbug-TestCommand)
    ("SCRIPT"
     (list
      (cons jbug-step-response "next")
      (cons jbug-step-response "next")
      (cons jbug-step-response "locals;break test.foo 44;continue")
      (cons jbug-breakpointEntered-response "locals;stack;next")
      (cons jbug-step-response "into")
      (cons jbug-step-response "next")
      (cons jbug-step-response "stack;locals;threads;breaks;next")
      (cons jbug-step-response "next")
      (cons jbug-step-response "classes;access test.foo$XThread first;continue")))))

  (message "running second  jbug-Testjbug %s" (length  jbug-responseTables))
   (jbug-add-commands
    (split-string "next" ";" 't)))
