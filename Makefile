VERSION:=20181114.2141
PACKAGE_NAME:=jbug-$(VERSION)
PACKAGE_DIR:=$(PACKAGE_NAME)

package: $(PACKAGE_DIR) jbug.jar
	cp src/main/elisp/jbug.org $(PACKAGE_DIR)/doc
	cp UsersManual.texi        $(PACKAGE_DIR)/doc
	cp UsersManual.info        $(PACKAGE_DIR)/doc
	cp UsersManual.org         $(PACKAGE_DIR)/doc
	cp src/main/java/tools.jar $(PACKAGE_DIR)
	cp src/main/java/jbug.jar  $(PACKAGE_DIR)
	cp src/main/elisp/jbug.el  $(PACKAGE_DIR)
	sed -re "s/VERSION/$(VERSION)/" jbug-package-template.el > $(PACKAGE_DIR)/jbug-package.el
	tar cvf $(PACKAGE_NAME).tar -C $(PACKAGE_DIR)/.. $(PACKAGE_NAME)

jbug.jar:
	cd ./src/main/java && $(MAKE) jar
	cp ./src/main/java/jbug.jar .

$(PACKAGE_DIR):
	mkdir $@
	mkdir $@/doc

clean:
	rm -f $(PACKAGE_NAME).tar
	rm -rf $(PACKAGE_DIR)

# end
