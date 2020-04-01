(defun jbugTest ()
  (interactive)
  (start-process "testProc" "*testProc*" "/home/jruby/tools/jdk1.8.0_131/bin/java" "-cp" ".:/home/jruby/tools/jdk1.8.0_131/lib/tools.jar" "-agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y" "test.foo" "3" "4")
  (load-file "/home/jruby/dev/javadebugger/src/main/elisp/jbug.el")
  (load-file "/home/jruby/dev/javadebugger/src/main/elisp/test.el")
  (setq jbug-proxy-command "java -cp /home/jruby/dev/javadebugger/src/main/java/jbug.jar:/home/jruby/dev/javadebugger/src/main/java/tools.jar com.github.jruby21.javadebugger.JavaDebuggerProxy")
  (jbug "/home/jruby/dev/javadebugger/src/main/java" "test.foo" "127.0.0.1" "8000")
)
(defun jbug-TestCommand (response script)
(if (not
     (and
      script
      (string= (car response) (car (car script)))))
    script
  (jbug-add-commands
   (split-string (cdr (car script)) ";" 't))
  (cdr script)))

(defun jbug-TestJbug ()
  (interactive)
  (jbug-addResponseTable
   "jbug-TestJbug environment"
   (ht
    (jbug-breakpointEntered-response
     `(lambda (env response)
        (ht-set env "SCRIPT" (jbug-TestCommand response (ht-get env "SCRIPT")))))
    (jbug-step-response
     `(lambda (env response)
        (ht-set env "SCRIPT" (jbug-TestCommand response (ht-get env "SCRIPT")))))
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

   (jbug-add-commands
    (split-string "next" ";" 't)))
