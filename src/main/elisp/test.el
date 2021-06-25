;;; test --- Test code for jbug, an Emacs based JAVA debugger  -*- lexical-binding: t; -*-

;;; Copyright (C) 2021 Jonathan Ruby

;; Author: Jonathan Ruby <jruby21@protonmail.com>
;; Maintainer: Jonathan Ruby <jruby21@protonmail.com>
;; Created: 18 August 2018
;; Keywords: languages,tools
;; Homepage: https://github.com/jruby21/javadebugger

;; This file is not part of GNU Emacs.

;; This file is free software

;;    This file is part of jbug.

;;    jbug is free software: you can redistribute it and/or modify
;;    it under the terms of the GNU General Public License as published by
;;    the Free Software Foundation, either version 3 of the License, or
;;    (at your option) any later version.

;;    jbug is distributed in the hope that it will be useful,
;;    but WITHOUT ANY WARRANTY; without even the implied warranty of
;;    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;    GNU General Public License for more details.

;;    You should have received a copy of the GNU General Public License
;;    along with jbug.  If not, see <https://www.gnu.org/licenses/>.

;;; Commentary:

;;    Test code for the jbug JAVA debugger
;;     Also a good example for using and programming the debugger.

;;; Code:

; jbugMode requires loading this emacs package:

;   dash

(require 'dash)

;;; Code:

(add-to-list 'load-path ".")

(require 'jbug)

(defun jbugTest (port)
  "Run the javadebugger test program.
Connect with the target program on port number PORT."
 (interactive "nPort number:")

 (message "new test")

  ;;; Start the program to be debugged.

  (start-process
   "testProc"
   "*testProc*"
   "java"
   "-cp"
   "/home/jruby/dev/javadebugger/src/main/java"
   (format "-agentlib:jdwp=transport=dt_socket,address=localhost:%d,server=y,suspend=y" port)
   "test.foo"
   "3"
   "4")

  (load-file "/home/jruby/dev/javadebugger/src/main/elisp/jbug.el")

  ;;; Set the test up once the javadebugger has been initialized.

  (add-hook
   `jbug-mode-hook
   (function jbug-TestWaitForInitialisation))

  ;;; Start the javadebugger.

    (setq jbug-proxy-command "java -cp /home/jruby/dev/javadebugger/src/main/java/jbug.jar com.github.jruby21.javadebugger.JavaDebuggerProxy")
  (jbug "/home/jruby/dev/javadebugger/src/main/java" "test.foo" "127.0.0.1" (number-to-string port)))

(defun jbug-TestWaitForInitialisation ()
  "Called from the javadebugger after the debugger has started and initialized.
Programs the javadebugger by adding a new response table.  Note
that we add responses to breakpoints and that the responses queue
debugger commands for execution."

  (jbug-addResponseTable
   "setup test script"
   (ht
    (jbug-breakpointEntered-response
     (function (lambda (_env resp)
       (let
           ((lm (jbug-locationMethod (-slice resp 8)))
            (ln (jbug-locationLineNumber (-slice resp 8)))
            (jbug-testThread  (jbug-threadID (-slice resp 2 8))))
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
            (format "threads;arguments * %s;arguments f %s;arguments f.a  %s;arguments arr.1 %s;arguments arr.5-60 %s;arguments arr.58 %s;this;down;up;arguments;modify test.foo b;continue"
                    jbug-testThread jbug-testThread jbug-testThread  jbug-testThread  jbug-testThread
                    jbug-testThread))))))))

    (jbug-accessWatchpoint-response
     (function (lambda (_env  _resp)
       ())))

    (jbug-modificationWatchpoint-response
     (function (lambda (_env  _resp)
        (jbug-addResponseCommand "access test.foo b;continue")))))))

;;; test.el ends here
