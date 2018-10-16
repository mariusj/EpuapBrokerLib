ePUAP Broker library
===

A library for communicating with the ePUAP platform.

The ePUAP platform provides several SOAP services. This library is a wrapper around these services.
In the `pl.gov.sejm.epuap.model` package there are model classes that represent e.g. document and attachment.
In the `pl.gov.sejm.epuap` package there is a `EpuapService` class that encapsulates common operations on ePUAP and two interfaces that have to be implemented:
- `Store` that provides access to underlying data store
- `EpuapConfig` that provides configuration


