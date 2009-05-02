/***
 * 
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. Neither the name of the
 * copyright holders nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package br.com.caelum.vraptor.http.ognl;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import ognl.Evaluation;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.SetPropertyAccessor;
import br.com.caelum.vraptor.Converter;
import br.com.caelum.vraptor.core.Converters;
import br.com.caelum.vraptor.ioc.Container;
import br.com.caelum.vraptor.validator.ValidationMessage;
import br.com.caelum.vraptor.vraptor2.Info;

public class SetAccessor extends SetPropertyAccessor {

	@SuppressWarnings("unchecked")
	@Override
	public Object getProperty(Map context, Object target, Object value) throws OgnlException {
		Set<?> set = (Set<?>) target;
		int index = (Integer) value;
		if(set.size()<=index) {
			return null;
		}
		Iterator<?> iterator = set.iterator();
		for(int i=0;i<index;i++) {
			iterator.next();
		}
		return iterator.next();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setProperty(Map context, Object target, Object key, Object value) throws OgnlException {
		Set<?> set = (Set<?>) target;
		int index = (Integer) key;
		for (int i = set.size(); i < index; i++) {
			set.add(null);
		}
		if (value instanceof String) {
			// it might be that suckable ognl did not call convert, i.e.: on the
			// values[i] = 2l in a List<Long>.
			// we all just looooove ognl.
			OgnlContext ctx = (OgnlContext) context;
			// if direct injecting, cannot find out what to do, use string
			if (ctx.getRoot() != target) {
				Evaluation eval = ctx.getCurrentEvaluation();
				Evaluation previous = eval.getPrevious();
				String fieldName = previous.getNode().toString();
				Object origin = previous.getSource();
				Method getter = ReflectionBasedNullHandler.findMethod(origin.getClass(), "get"
						+ Info.capitalize(fieldName), origin.getClass(), null);
				Type genericType = getter.getGenericReturnType();
				Class type;
				if (genericType instanceof ParameterizedType) {
					type = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[0];
				} else {
					type = (Class) genericType;
				}
				if (!type.equals(String.class)) {
					// suckable ognl doesnt support dependency injection or
					// anything alike... just that suckable context... therefore
					// procedural
					// programming and ognl live together forever!
					Container container = (Container) context.get(Container.class);
					Converter<?> converter = container.instanceFor(Converters.class).to(type, container);
					List<ValidationMessage> errors = (List<ValidationMessage>) context.get("errors");
					ResourceBundle bundle = (ResourceBundle) context.get(ResourceBundle.class);
					Object result = converter.convert((String) value, type, bundle);
					setAt(set, index, result);
					return;
				}
			}
		}
		setAt(set, index, value);
	}

	private void setAt(Set<?> set, int index, Object value) {
		LinkedHashSet s = (LinkedHashSet) set;
		Iterator<?> iterator = set.iterator();
		for(int i=0;i<index;i++) {
			iterator.next();
		}
		List following = new ArrayList();
		while(iterator.hasNext()) {
			following.add(iterator.next());
			iterator.remove();
		}
		s.add(value);
		s.addAll(following);
	}

}
