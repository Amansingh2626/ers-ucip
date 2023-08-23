FROM sdsdockerhub/seamless-jdk:latest

ADD target/rpm/BUILD/ers-uciplink-*/lib/uciplink/* /opt/seamless/lib/uciplink/
RUN addgroup -S ersadmin && adduser -S admin -G ersadmin

RUN mkdir -p /opt/seamless/conf/uciplink
RUN mkdir -p /var/seamless/log/

RUN chown -R admin:ersadmin /opt/seamless/
RUN chown -R admin:ersadmin /var/seamless/
USER admin

VOLUME /var/seamless/log/uciplink
CMD ["java","-server","-cp","/opt/seamless/lib/uciplink/*:/opt/seamless/conf/uciplink","com.seamless.ers.links.uciplink.UCIPLink","uciplink","/var/seamless/log/uciplink/init.out"]
