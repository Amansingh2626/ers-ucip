#
# $Id$
#
package Seamless::ERSInstall::uciplink;

use strict;
use warnings;
use base qw(Seamless::ERSInstall);

use Seamless::Common;

sub new {
	my $class = shift;
	my $self = $class->SUPER::new(@_);
	$self->{config}->{file} = "/opt/seamless/conf/uciplink/uciplink.properties";
	$self->{databases} = [
	{
		name => "uciplink",
		url =>  "uciplink.reference_generator.db_url",
		user => "uciplink.reference_generator.db_user",
		pass => "uciplink.reference_generator.db_password"
	}
	];
	bless $self, $class;
	return $self;
}

1;

