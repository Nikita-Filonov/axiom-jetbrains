package testify

import (
	"testing"

	"github.com/stretchr/testify/suite"
)

// Testify suite – must NOT be detected as Axiom.

func TestPlainTestifySuite(t *testing.T) {
	suite.Run(t, new(plainSuite))
}

type plainSuite struct {
	suite.Suite
}

func (s *plainSuite) TestNothing() {}
