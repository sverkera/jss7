/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.map.primitives;

import java.io.IOException;
import java.util.ArrayList;

import org.mobicents.protocols.asn.AsnException;
import org.mobicents.protocols.asn.AsnInputStream;
import org.mobicents.protocols.asn.AsnOutputStream;
import org.mobicents.protocols.asn.Tag;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParsingComponentException;
import org.mobicents.protocols.ss7.map.api.MAPParsingComponentExceptionReason;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.api.primitives.MAPPrivateExtension;

/**
 * @author sergey vetyutnev
 * 
 */
public class MAPExtensionContainerImpl extends MAPPrimitiveBase implements MAPExtensionContainer {

	protected static final int PRIVATEEXTENSIONLIST_REF_TAG = 0x00;
	protected static final int PSCEXTENSIONS_REF_TAG = 0x01;

	private ArrayList<MAPPrivateExtension> privateExtensionList;
	private byte[] pcsExtensions;

	public MAPExtensionContainerImpl() {
	}

	public MAPExtensionContainerImpl(ArrayList<MAPPrivateExtension> privateExtensionList, byte[] pcsExtensions) {
		this.privateExtensionList = privateExtensionList;
		this.pcsExtensions = pcsExtensions;
	}

	@Override
	public ArrayList<MAPPrivateExtension> getPrivateExtensionList() {
		return this.privateExtensionList;
	}

	@Override
	public void setPrivateExtensionList(ArrayList<MAPPrivateExtension> privateExtensionList) {
		this.privateExtensionList = privateExtensionList;
	}

	@Override
	public byte[] getPcsExtensions() {
		return this.pcsExtensions;
	}

	@Override
	public void setPcsExtensions(byte[] pcsExtensions) {
		this.pcsExtensions = pcsExtensions;
	}


	@Override
	public int getTag() throws MAPException {
		return Tag.SEQUENCE;
	}

	@Override
	public int getTagClass() {
		return Tag.CLASS_UNIVERSAL;
	}

	@Override
	public boolean getIsPrimitive() {
		return false;
	}


	@Override
	public void decodeAll(AsnInputStream ansIS) throws MAPParsingComponentException {
		// Definition from GSM 09.02 version 5.15.1 Page 690
		// extensionContainer SEQUENCE {
		// privateExtensionList [0] IMPLICIT SEQUENCE ( SIZE( 1 .. 10 ) ) OF
		// SEQUENCE {
		// extId MAP-EXTENSION .&extensionId ( {
		// ,
		// ...} ) ,
		// extType MAP-EXTENSION .&ExtensionType ( {
		// ,
		// ...} { @extId } ) OPTIONAL} OPTIONAL,
		// pcs-Extensions [1] IMPLICIT SEQUENCE {
		// ... } OPTIONAL,
		// ... } OPTIONAL,
		// ... }
		
		try {
			AsnInputStream ais = ansIS.readSequenceStream();
			this._decode(ais);
		} catch (IOException e) {
			throw new MAPParsingComponentException("IOException when decoding ExtensionContainer: " + e.getMessage(), e,
					MAPParsingComponentExceptionReason.MistypedParameter);
		} catch (AsnException e) {
			throw new MAPParsingComponentException("AsnException when decoding ExtensionContainer: " + e.getMessage(), e,
					MAPParsingComponentExceptionReason.MistypedParameter);
		}
	}

	@Override
	public void decodeData(AsnInputStream ansIS, int length) throws MAPParsingComponentException {

		try {
			AsnInputStream ais = ansIS.readSequenceStreamData(length);
			this._decode(ais);
		} catch (IOException e) {
			throw new MAPParsingComponentException("IOException when decoding ExtensionContainer: " + e.getMessage(), e,
					MAPParsingComponentExceptionReason.MistypedParameter);
		} catch (AsnException e) {
			throw new MAPParsingComponentException("AsnException when decoding ExtensionContainer: " + e.getMessage(), e,
					MAPParsingComponentExceptionReason.MistypedParameter);
		}
	}
	
	private void _decode(AsnInputStream ansIS) throws MAPParsingComponentException, IOException, AsnException {

		this.privateExtensionList = null;
		this.pcsExtensions = null;

		while (true) {
			if (ansIS.available() == 0)
				break;

			int tag = ansIS.readTag();
			if (tag == MAPExtensionContainerImpl.PRIVATEEXTENSIONLIST_REF_TAG && ansIS.getTagClass() == Tag.CLASS_CONTEXT_SPECIFIC) {
				
				if(ansIS.isTagPrimitive())
					throw new MAPParsingComponentException("Error while ExtensionContainer decoding: privateExtensionList is primitive",
							MAPParsingComponentExceptionReason.MistypedParameter);
				if (this.privateExtensionList != null)
					throw new MAPParsingComponentException("Error while ExtensionContainer decoding: More than one PrivateExtensionList has found",
							MAPParsingComponentExceptionReason.MistypedParameter);

				AsnInputStream localAis2 = ansIS.readSequenceStream();
				this.privateExtensionList = new ArrayList<MAPPrivateExtension>();
				while (localAis2.available() > 0) {
					tag = localAis2.readTag();
					if (tag != Tag.SEQUENCE || localAis2.getTagClass() != Tag.CLASS_UNIVERSAL || localAis2.isTagPrimitive())
						throw new MAPParsingComponentException("Error while ExtensionContainer decoding: Bad tag, tagClass or primitiveFactor of PrivateExtension",
								MAPParsingComponentExceptionReason.MistypedParameter);
					if (this.privateExtensionList.size() >= 10)
						throw new MAPParsingComponentException("More then 10 PrivateExtension found when PrivateExtensionList decoding",
								MAPParsingComponentExceptionReason.MistypedParameter);

					MAPPrivateExtensionImpl privateExtension = new MAPPrivateExtensionImpl();
					privateExtension.decodeAll(localAis2);
					this.privateExtensionList.add(privateExtension);
				}
			} else if (tag == MAPExtensionContainerImpl.PSCEXTENSIONS_REF_TAG && ansIS.getTagClass() == Tag.CLASS_CONTEXT_SPECIFIC) {
				
				if(ansIS.isTagPrimitive())
					throw new MAPParsingComponentException("Error while PCS-Extensions decoding: PCS-Extensions is primitive",
							MAPParsingComponentExceptionReason.MistypedParameter);
				if (this.pcsExtensions != null)
					throw new MAPParsingComponentException("Error while PCS-Extensions decoding: More than one PCS-Extensions has found",
							MAPParsingComponentExceptionReason.MistypedParameter);

				this.pcsExtensions = ansIS.readSequence();
			}
		}
	}
	
	@Override
	public void encodeAll(AsnOutputStream asnOs) throws MAPException {
		
		this.encodeAll(asnOs, Tag.CLASS_UNIVERSAL, Tag.SEQUENCE);
	}
	
	@Override
	public void encodeAll(AsnOutputStream asnOs, int tagClass, int tag) throws MAPException {
		
		try {
			asnOs.writeTag(tagClass, false, tag);
			int pos = asnOs.StartContentDefiniteLength();
			this.encodeData(asnOs);
			asnOs.FinalizeContent(pos);
		} catch (AsnException e) {
			throw new MAPException("AsnException when encoding ExtensionContainer: " + e.getMessage(), e);
		}
	}

	@Override
	public void encodeData(AsnOutputStream asnOs) throws MAPException {
		
		if (this.privateExtensionList == null && this.pcsExtensions == null)
			throw new MAPException(
					"Error when encoding ExtensionContainer: Both PrivateExtensionList and PcsExtensions are empty when ExtensionContainer encoding");
		if (this.privateExtensionList != null && (this.privateExtensionList.size() == 0 || this.privateExtensionList.size() > 10))
			throw new MAPException(
					"Error when encoding ExtensionContainer: PrivateExtensionList must contains from 1 to 10 elements when ExtensionContainer encoding");

		try {
			
			if (this.privateExtensionList != null) {
				asnOs.writeTag(Tag.CLASS_CONTEXT_SPECIFIC, false, PRIVATEEXTENSIONLIST_REF_TAG);
				int pos2 = asnOs.StartContentDefiniteLength();
				
				for (MAPPrivateExtension pe : this.privateExtensionList) {
					pe.encodeAll(asnOs);
				}				

				asnOs.FinalizeContent(pos2);
			}
			
			if (this.pcsExtensions != null) {
				asnOs.writeTag(Tag.CLASS_CONTEXT_SPECIFIC, false, PSCEXTENSIONS_REF_TAG);
				int pos2 = asnOs.StartContentDefiniteLength();

				asnOs.write(this.pcsExtensions);

				asnOs.FinalizeContent(pos2);
			}			
		} catch (AsnException e) {
			throw new MAPException("AsnException when encoding ExtensionContainer: " + e.getMessage(), e);
		}
	}

	@Deprecated
	public void decode(AsnInputStream lis, int tagClass, boolean isPrimitive, int masterTag, int length) throws MAPParsingComponentException {
		
		try {
			this._decode(lis);
		} catch (IOException e) {
			throw new MAPParsingComponentException("IOException when decoding ExtensionContainer: " + e.getMessage(), e,
					MAPParsingComponentExceptionReason.MistypedParameter);
		} catch (AsnException e) {
			throw new MAPParsingComponentException("AsnException when decoding ExtensionContainer: " + e.getMessage(), e,
					MAPParsingComponentExceptionReason.MistypedParameter);
		}		
	}
	
	@Deprecated
	public void encode(AsnOutputStream asnOS) throws MAPException {
		
		this.encodeData(asnOS);
		
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ExtensionContainer [");
		
		if (this.privateExtensionList != null && this.privateExtensionList.size() > 0) {
			for(MAPPrivateExtension pe : this.privateExtensionList) {
				sb.append("\n");
				sb.append(pe.toString());
			}
		}
		
		if (this.pcsExtensions != null) {
			sb.append("\nPcsExtensions=");
			sb.append(this.ArrayToString(this.pcsExtensions));
		}

		sb.append("]");

		return sb.toString();
	}
	
	private String ArrayToString(byte[] array) {
		StringBuilder sb = new StringBuilder();
		int i1 = 0;
		for (byte b : array) {
			if (i1 == 0)
				i1 = 1;
			else
				sb.append(", ");
			sb.append(b);
		}
		return sb.toString();
	}
}