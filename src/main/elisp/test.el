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


(defvar jbug-testState    0)
(defvar jbug-testThread 0)
(defvar jbug-test-steps   ())

(defun jbug-testStepResp (resp com)
  (let ((tr (-slice resp 1 7)))
    (message "teststateresp %s %s %s %s" (car resp) resp com tr)
    (when
        (and
         (string= (car resp)  jbug-step-response)
         (= jbug-testThread (threadID tr)))
      (jbug-add-commands (split-string com ";" 't))
      (setq jbug-testState (1+ jbug-testState)))))

(defun jbug-testResponse (env response)
  (let ((resp (mapcar 's-trim (split-string response ","))))
    (message "%s, %s, %s" (car resp) (-slice resp 8) (-slice resp 2 8))
    (cond
     ((and
       (= jbug-testState 0)
       (string= (car resp)  jbug-breakpointEntered-response)
       (string= "main"   (locationMethod (-slice resp 8))))
      (let ((tr             (-slice resp 2 8)))
        (setq jbug-testThread (threadID tr))
        (message "%s | %s" tr jbug-testThread)
;;        (jbug-add-commands
;;         (split-string (format "next %s" jbug-testThread) ";" 't))
        (message "end %s"  jbug-testState)
        (setq jbug-testState 1)))

     ((or (= jbug-testState 1) (= jbug-testState 2))
      (jbug-testStepResp resp  (format "next %s" jbug-testThread)))

     ((= jbug-testState 3)
      (jbug-testStepResp     resp     (format "locals %s 0;break test.foo 44;continue"  jbug-testThread)))

     ((and
       (= jbug-testState 4)
       (string= (car resp) jbug-breakpointEntered-response)
       (string= "44"   (locationLineNumber (-slice resp 8))))
      (let ((tr             (-slice resp 2 8)))
        (setq jbug-testThread (threadID tr))
        (jbug-add-commands
         (split-string (format "into %s" jbug-testThread) ";" 't))
        (setq jbug-testState 5))))))

(defun jbug-TestWaitForInitialisation ()

  (setq
   jbug-test-steps
   (list
    (list jbug-breakpointEntered-response "main" "" `(format "next %s" jbug-testThread))
    (list jbug-step-response `(format "next %s" jbug-testThread))
    (list jbug-step-response  `(format "next %s" jbug-testThread))
    (list jbug-step-response  `(format "locals * %s 0;break test.foo 44;continue"  jbug-testThread))
    (list jbug-breakpointEntered-response "" "44"
          `(format "locals * %s 0;stack %s;next %s" jbug-testThread jbug-testThread jbug-testThread))
    (list jbug-step-response  `(format "next %s" jbug-testThread))
    (list jbug-step-response  `(format "into %s" jbug-testThread))
    (list jbug-step-response  `(format "next %s" jbug-testThread))
    (list jbug-step-response  `(format "next %s" jbug-testThread))
    (list jbug-step-response  `(format "next %s" jbug-testThread))
    (list jbug-step-response  `(format "classes;access test.foo$XThread first"))))


  (jbug-addResponseTable
   "setup test script"
   (ht
    (jbug-breakpointEntered-response
     `(lambda (env resp)
        (let* ((step (car jbug-test-steps)))
          (when
              (and
               (string= jbug-breakpointEntered-response (car step))
               (or
                (string= (nth 1 step)   (locationMethod (-slice resp 8)))
                (string= (nth 2 step)   (locationLineNumber (-slice resp 8)))))
            (setq jbug-testThread (threadID (-slice resp 2 8)))
            (setq jbug-responseCommands
                  (cons
                   (eval (nth 3 step))
                   jbug-responseCommands))
            (setq jbug-test-steps (cdr jbug-test-steps))
            (if (not jbug-test-steps) (jbug-removeResponseTable env))))))
    (jbug-step-response
     `(lambda (env resp)
        (let* ((step (car jbug-test-steps)))
          (when
              (and
               (string= jbug-step-response (car step))
               (string= jbug-testThread (threadID (-slice resp 1 7))))
            (setq jbug-responseCommands
                  (cons
                   (eval (nth 1 step))
                   jbug-responseCommands))
            (setq jbug-test-steps (cdr jbug-test-steps))
            (if (not jbug-test-steps) (jbug-removeResponseTable env)))))))))
